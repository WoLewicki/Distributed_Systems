import socket, sys, time, random, queue, datetime


MCAST_GRP = '224.5.8.14'
MCAST_PORT = 5820
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 32)


lstcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
sstcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
lsudp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
ssudp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
client_id = ''
ip_addr = ''
nei_port = ''
list_port = ''
neighbour = None
nodes = 1
news_queue = queue.Queue()
is_data_awaiting = False
data_awaiting = ''
has_token = False


# data format : [0:5] - type(start, conne, passe) , [5:7] - dest_client_id (or token info in start)
# , [7:9] - send_client_id, [9:13] - list_port, [13:17] - new_port (only in conne)

def join_ring_tcp():
    print('Trying to join the ring in ' + client_id)
    if has_token:
        data = 'start01' + client_id + list_port
    else:
        data = 'start00' + client_id + list_port
    sstcp.connect(neighbour)
    sstcp.sendall(data.encode('utf-8'))
    sstcp.close()
    lstcp.bind((ip_addr, int(list_port)))
    lstcp.listen()
    print('Sent start msg to ' + nei_port)


def join_ring_udp():
    print('Trying to join the ring in ' + client_id)
    if has_token:
        data = 'start01' + client_id + list_port
    else:
        data = 'start00' + client_id + list_port
    ssudp.sendto(data.encode('utf-8'), neighbour)
    lsudp.bind((ip_addr, int(list_port)))
    print('Sent start msg to ' + nei_port)


def first_one_tcp():
    global neighbour, nodes, ip_addr, nei_port
    ip_addr = '127.0.0.1'
    lstcp.bind((ip_addr, int(list_port)))
    lstcp.listen()
    if has_token:
        conn, addr = lstcp.accept()
        data = conn.recv(1024)
        conn.close()
        data = data.decode('utf-8')
        nei_port = get_list_port(data)
        neighbour = (ip_addr, nei_port)
        nodes += 1
        data = 'passe02' + client_id + list_port
        sstcp.connect(neighbour)
        sstcp.sendall(data.encode('utf-8'))
        sstcp.close()
        print('Sent first msg in ' + client_id)


def first_one_udp():
    global neighbour, nodes, ip_addr, nei_port
    ip_addr = '127.0.0.1'
    lsudp.bind((ip_addr, int(list_port)))
    if has_token:
        data, addr = lsudp.recvfrom(1024)
        data = data.decode('utf-8')
        nei_port = get_list_port(data)
        neighbour = (ip_addr, nei_port)
        nodes += 1
        data = 'passe02' + client_id + list_port
        ssudp.sendto(data.encode('utf-8'), neighbour)
        print('Sent first msg in ' + client_id)


def accept_new_client(data):
    global neighbour, nei_port
    print('Accepting new client in ' + client_id)
    nei_port = get_new_port(data)
    neighbour = (ip_addr, nei_port)


def get_list_port(data):
    return int(data[9:13])


def get_new_port(data):
    return int(data[13:17])


def create_random_passe():
    r = random.randint(1, nodes)
    if r < 10:
        return 'passe0' + str(r) + client_id + str(list_port)
    else:
        return 'passe' + str(r) + client_id + str(list_port)


def prep_data(data):
    global nodes, data_awaiting, is_data_awaiting
    print('Got ' + data + ' in ' + client_id + ' (port ' + list_port + ')' + ' timestamp: ' + str(datetime.datetime.now()))
    sock.sendto(client_id.encode('utf-8'), (MCAST_GRP, MCAST_PORT))  # multicast
    if data[7:9] == client_id:
        print(
            'Message came back to the sender, sending new passe msg in ' + client_id + ' (port ' + list_port + ')' + ' timestamp: ' + str(
                datetime.datetime.now()))
    if data[0:5] == 'start':
        nodes += 1
        if data[5:7] == '01':  # start of communication, so no sense in putting anything on queue
            if get_list_port(data) == nei_port:  # second person, he listens where we send
                data = 'passe02' + client_id + list_port
            else:
                data = 'conne' + client_id + client_id + str(list_port) + str(get_list_port(data))
        else:
            news_queue.put(get_list_port(data))
            return None
    elif data[0:5] == 'conne':
        nodes += 1
        if get_new_port(data) == int(list_port):  # got conne from before communication, send passe to prevent errors
            data = create_random_passe()
        elif get_list_port(data) == int(nei_port):
            cid = data[5:7]
            accept_new_client(data)
            data = 'passe' + cid + client_id + str(list_port)
    elif data[0:5] == 'passe':
        if not news_queue.empty():
            is_data_awaiting = True
            data_awaiting = data
            data = 'conne' + client_id + client_id + str(list_port) + str(news_queue.get())
        else:
            if data[5:7] == client_id:
                if is_data_awaiting:
                    print('There was data awaiting in ' + client_id)
                    is_data_awaiting = False
                    data = data_awaiting
                else:
                    data = create_random_passe()
    return data


def process_data_tcp():
    global sstcp, neighbour, nei_port
    while True:
        conn, addr = lstcp.accept()
        data = conn.recv(1024)
        conn.close()
        data = data.decode('utf-8')
        if not neighbour:
            nei_port = get_list_port(data)
            neighbour = (ip_addr, nei_port)
        data = prep_data(data)
        if not data:
            pass
        else:
            time.sleep(1)
            sstcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sstcp.connect(neighbour)
            sstcp.sendall(data.encode('utf-8'))
            sstcp.close()
            print('Sent ' + data + ' to ' + str(nei_port) + ' in ' + client_id + ' (port ' + list_port + ')')


def process_data_udp():
    global nei_port, neighbour
    while True:
        data, addr = lsudp.recvfrom(1024)
        data = data.decode('utf-8')
        if not neighbour:
            nei_port = get_list_port(data)
            neighbour = (ip_addr, nei_port)
        data = prep_data(data)
        if not data:
            pass
        else:
            time.sleep(1)
            ssudp.sendto(data.encode('utf-8'), neighbour)
            print('Sent ' + data + ' to ' + str(nei_port) + ' in ' + client_id + ' (port ' + list_port + ')')


def start_client(pr):
    if pr == 'tcp':
        if ip_addr == '':
            first_one_tcp()
        else:
            join_ring_tcp()
        process_data_tcp()
    elif pr == 'udp':
        if ip_addr == '':
            first_one_udp()
        else:
            join_ring_udp()
        process_data_udp()


if len(sys.argv) == 6:
    client_id = sys.argv[1]
    list_port = sys.argv[2]
    ip_addr = sys.argv[3].split(':')[0]
    nei_port = sys.argv[3].split(':')[1]
    if nei_port is not '':
        neighbour = (ip_addr, int(nei_port))
    protocol = sys.argv[4]
    if sys.argv[5] == '1':
        has_token = True
    start_client(protocol)

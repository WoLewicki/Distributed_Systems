from kazoo.client import KazooClient
from kazoo.client import KazooState
import logging
import sys
from subprocess import Popen


opened = False
app_process = None
highest = 0


def my_listener(state):
    if state == KazooState.LOST:
        print("Lost")
    elif state == KazooState.SUSPENDED:
        print("Suspensed")
    else:
        print("Connected")
        

def open_app(app):
    print("Opening app")
    return Popen(app)


def close_app():
    print("Closing app")
    app_process.kill()
   
    
def recursively_print(zk, path, deep):
    for child in zk.get_children(path):
        print("-"*deep + child)
        recursively_print(zk, path+'/'+child, deep+1)


def print_tree(zk):
    if not zk.exists('/z'):
        print("There is no 'z' tree")
    else:
        print("z")
        for child in zk.get_children('/z'):
            print("-" + child)
            recursively_print(zk, '/z/'+child, 2)


def create_node(zk, s):
    print("Creating node.")
    node = s.split(" ")[1]
    if not zk.exists(node):
        zk.create(node)
    else:
        print("Node exists.")


def delete_node(zk, s):
    print("Deleting node.")
    node = s.split(" ")[1]
    if zk.exists(node):
        if not zk.get_children(node):
            zk.delete(node)
        else:
            print("There are children in there!")


def count_all_children(zk, path):
    value = 0
    for child in zk.get_children(path):
        value += count_my_children(zk, path+'/'+child)
    return value


def count_my_children(zk, path):
    value = 0
    for child in zk.get_children(path):
        value += count_my_children(zk, path+'/'+child)
    return 1+value


def watch_all(zk, path):
    for child in zk.get_children(path):
        watch_my_children(zk, path+'/'+child)
        
    
def watch_my_children(zk, my_path):
    @zk.ChildrenWatch(my_path)
    def watch_z(z_children):
        global highest
        my_val = count_all_children(zk, '/z') 
        if my_val > highest:
            print("There are now %s children in 'z'" % my_val)
            highest = my_val
        if my_val < highest:
            highest = my_val
        for child in zk.get_children(my_path):
            watch_my_children(zk, my_path+'/'+child)


def main():
    if len(sys.argv) < 3:
        print("Pass port and app.")
        exit(1)
    port = sys.argv[1]
    zk = KazooClient(hosts='127.0.0.1:' + port)
    app = sys.argv[2]
    zk.start()
    
    @zk.ChildrenWatch('/')
    def watch_main(c):
        global opened, app_process
        if 'z' in c:
            @zk.ChildrenWatch('/z')
            def watch_z(z_children):
                count_all_children(zk, '/z')
                watch_my_children(zk, '/z')
            if not opened:
                app_process = open_app(app)
                opened = True
        if 'z' not in c:
            if opened:
                close_app()
                app_process = None
                opened = False
                
    logging.basicConfig()
    try:
        while True:
            s = input("p - print znode tree\n")
            if s.split(" ")[0] == 'create':
                create_node(zk, s)
            if s == 'p':
                print_tree(zk)
            elif s.split(" ")[0] == 'delete':
                delete_node(zk, s)
            else:
                pass
    except KeyboardInterrupt:
        zk.stop()


if __name__ == "__main__":
    main()

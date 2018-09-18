import psycopg2
from psycopg2 import Error

try:
    conn = psycopg2.connect(user="pi",
                            password="POPROTOTYPE98",
                            host="192.168.137.78",
                            port="5432",
                            database = "postgres")
    cur = conn.cursor()

    cur.execute("""insert into test values (7868,'Roystan')""")
    conn.commit()
    cur.execute('select * from test')

    results = cur.fetchall()

    for result in results:
        print(result)
except (Exception,psycopg2.DatabaseError) as error:
    if(conn):
        conn.rollback()
    print("Failed inserting record into mobile table {}".format(error))
finally:
    if(conn):
        cur.close()
        conn.close()
        print("Closed")

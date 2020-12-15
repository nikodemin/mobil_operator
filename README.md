Mobile operator
===========================
This is a sharding application containing 2 types of persistent sharding actors.
First represent user and second one - account of the mobile operator.

### Used Technologies
<ul>
    <li>akka-persistence</li>
    <li>akka-sharding</li>
    <li>akka-projection</li>
    <li>tapir</li>
    <li>slick</li>
</ul>

### How to Run
<ul>
    <li>start cassandra using <code>sudo docker run -p 9042:9042 cassandra</code></li>
    <li>start postgres on port 5432</li>
    <li>run <code>sbt universal:stage</code></li>
    <li>go to nodes/cmd1 directory and run <code>./run.sh</code></li>
    <li>go to nodes/query1 directory and run <code>./run.sh</code></li>
</ul>
The Swagger UI will be available on <code>http://localhost:9091/docs</code> and <code>http://localhost:9093/docs</code>

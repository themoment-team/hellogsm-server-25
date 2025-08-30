from diagrams import Diagram, Cluster, Edge
from diagrams.aws.compute import EC2, ElasticBeanstalk
from diagrams.aws.database import RDS
from diagrams.aws.network import ALB, InternetGateway, NATGateway, VPC
from diagrams.aws.devtools import Codedeploy
from diagrams.aws.storage import S3
from diagrams.aws.management import Cloudwatch
from diagrams.onprem.client import User
from diagrams.onprem.vcs import Github

with Diagram("HelloGSM-2025 Cloud Diagram", show=False):

    # CI/CD
    with Cluster("CI/CD"):
        github = Github("GitHub")
        s3 = S3("s3")
        codedeploy = Codedeploy("code deploy")
        github >> s3 >> codedeploy

    # Alarm
    with Cluster("Alarm"):
        cloudwatch = Cloudwatch("Alarm")
        discord = User("Discord")
        cloudwatch >> Edge(label="Lambda") >> discord

    # VPC
    with Cluster("VPC"):

        with Cluster("Prod Public subnet"):
            prod_bastion = EC2("bastion")
            prod_nat_gateway = NATGateway("nat gateway")

        with Cluster("Prod Private subnet"):
            prod_ec2 = EC2("Spring Application\nRedis")
            mysql = RDS("MySQL Server")
            prod_ec2 >> mysql

        with Cluster("Dev Public subnet"):
            dev_bastion = EC2("bastion")
            dev_nat_gateway = NATGateway("nat gateway")

        with Cluster("Dev Private subnet"):
            dev_ec2 = EC2("Spring Application\nMySQL\nRedis")

        internet_gateway = InternetGateway("internet gateway")
        alb = ALB("ALB")

        internet_gateway >> alb
        alb >> prod_ec2
        alb >> dev_ec2

        prod_bastion >> mysql
        prod_ec2 >> prod_nat_gateway

        dev_bastion >> dev_ec2
        dev_ec2 >> dev_nat_gateway


    user = User("User")
    user >> internet_gateway

    codedeploy >> prod_ec2
    codedeploy >> dev_ec2

    cloudwatch >> alb

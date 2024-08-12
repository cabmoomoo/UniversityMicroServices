# University Microservices

A collection of practice microservices built to model what a university might use to store and view students, professors, and courses. This collection was built to run on the AWS cloud, with each service/package being ran on its own EC2 instance and the DAO acting as a gateway for an AWS RDS. The Discovery Service acts as the linchpin. All for services self-register, and three of the services plus the API Gateway leverage the Discovery Service to find the service they require.

## Project Status

This project was intended mainly to be practice. As such, all features currently implemented should be fully functional, but the project as a whole is far from feature complete. Neither record services are fully capable of CRUD operations, but the API Gateway is able to successfully forward messages to them. At this time, I do not expect to be adding more features, as I've already got what I wanted out of this project. Setting up the required VPC, subnets, RDS, etc. to make a fully functional AWS cloud system was interesting enough. In the future, these services will likely be best applied using something like Beanstalk.

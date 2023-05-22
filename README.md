# H3 User Defined Function(s) (UDF) for Athena

Forked from [aws-samples/h3-udf-for-athena](https://github.com/aws-samples/h3-udf-for-athena) for use at Neighbor. Differences include the following changes:

- Readded `@Test` annotations for JUnit tests
- Modified functions to work with GeoJSON-style WKT text, where coordinates are in the form of `longitude` `latitude`, as opposed to `latitude` `longitude`
- Using the [NGA Simple Features Java](https://github.com/ngageoint/simple-features-java) to perform WKT parsing and Geometry manipulation

---

This repository contains example code to support the blog post [Extend geospatial queries in Amazon Athena with UDFs and AWS Lambda](https://aws.amazon.com/blogs/big-data/extend-geospatial-queries-in-amazon-athena-with-udfs-and-aws-lambda/). The UDF makes it straightforward for [Amazon Athena](https://aws.amazon.com/athena/) to find out which [Uber H3](https://eng.uber.com/h3/) hexagon a pair of (lat, long) coordinates are in. This can be used for subsequent analysis and visualisation.

We include code for the [AWS Lambda](https://aws.amazon.com/lambda/) function that powers the new Athena UDF. Also included is an example Jupyter Notebook which may be used in an [Amazon SageMaker Notbook Instance](https://docs.aws.amazon.com/sagemaker/latest/dg/nbi.html) to render a choropleth map.

![Map](./media/earthquake_map.png "Example map.")

## How to use

- Package the UDF by going to the `udf` directory and running `mvn clean package`
- Run tests by going to the `udf` directory and running `mvn clean test`
- Run `cdk deploy` in the infrastructure directory of the repository

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the MIT-0 License. See the LICENSE file.

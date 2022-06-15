package com.epam.learn.microservices.fundamentals.resource.service.config

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration(proxyBeanMethods = false)
class AWSConfig {

    @ConstructorBinding
    @ConfigurationProperties("configuration.aws")
    data class Props(
        val region: String,
    )

    @ConstructorBinding
    @ConfigurationProperties("configuration.aws.s3")
    data class S3Props(
        val url: String,
        val bucketName: String,
        val accessKey: String,
        val secretKey: String,
    )

    @Bean
    fun amazonS3(props: Props, s3Props: S3Props): AmazonS3 {
        val credentials = AWSStaticCredentialsProvider(
            BasicAWSCredentials(
                s3Props.accessKey,
                s3Props.secretKey
            )
        )
        val endpointConfiguration = AwsClientBuilder.EndpointConfiguration(
            s3Props.url,
            props.region
        )
        val amazonS3 = AmazonS3ClientBuilder.standard()
            .withCredentials(credentials)
            .withEndpointConfiguration(endpointConfiguration)
            .build()

        if (!amazonS3.doesBucketExistV2(s3Props.bucketName)) {
            amazonS3.createBucket(s3Props.bucketName)
        }
        return amazonS3
    }
}

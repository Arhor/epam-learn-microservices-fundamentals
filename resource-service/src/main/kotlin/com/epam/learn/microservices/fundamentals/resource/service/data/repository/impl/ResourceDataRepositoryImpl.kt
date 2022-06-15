package com.epam.learn.microservices.fundamentals.resource.service.data.repository.impl

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.epam.learn.microservices.fundamentals.resource.service.config.AmazonS3Config
import com.epam.learn.microservices.fundamentals.resource.service.data.model.ResourceMeta
import com.epam.learn.microservices.fundamentals.resource.service.data.repository.ResourceDataRepository
import org.springframework.stereotype.Repository
import java.io.ByteArrayInputStream
import java.io.InputStream

@Repository
class ResourceDataRepositoryImpl(
    private val amazonS3: AmazonS3,
    private val amazonS3ConfigProps: AmazonS3Config.Props,
) : ResourceDataRepository {

    override fun upload(filename: String, data: ByteArray): ResourceMeta {
        val inputStream = ByteArrayInputStream(data)
        val objectMetadata = ObjectMetadata().apply { contentLength = data.size.toLong() }

        val url = amazonS3ConfigProps.url
        val bucketName = amazonS3ConfigProps.bucketName
        val path = "$url/$bucketName/$filename"

        val result = amazonS3.putObject(bucketName, filename, inputStream, objectMetadata)

        return ResourceMeta(
            filename = path,
            uploaded = result != null,
            length = result?.metadata?.contentLength ?: 0
        )
    }

    override fun download(filename: String): InputStream {
        val bucketName = amazonS3ConfigProps.bucketName
        val s3Object = amazonS3.getObject(bucketName, filename)

        return s3Object.objectContent
    }

    override fun delete(filenames: Iterable<String>): List<String> {
        val bucketName = amazonS3ConfigProps.bucketName
        val filesToDelete = filenames.toList().toTypedArray()
        val request = DeleteObjectsRequest(bucketName).withKeys(*filesToDelete)

        val result = amazonS3.deleteObjects(request)

        return result.deletedObjects.map { it.key }
    }
}

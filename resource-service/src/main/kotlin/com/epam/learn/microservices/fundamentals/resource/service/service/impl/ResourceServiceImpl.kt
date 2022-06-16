package com.epam.learn.microservices.fundamentals.resource.service.service.impl

import com.epam.learn.microservices.fundamentals.resource.service.data.model.ResourceMeta
import com.epam.learn.microservices.fundamentals.resource.service.data.repository.ResourceDataRepository
import com.epam.learn.microservices.fundamentals.resource.service.data.repository.ResourceMetaRepository
import com.epam.learn.microservices.fundamentals.resource.service.service.ResourceService
import com.epam.learn.microservices.fundamentals.resource.service.service.dto.ResourceDTO
import com.epam.learn.microservices.fundamentals.resource.service.service.exception.EntityDuplicateException
import com.epam.learn.microservices.fundamentals.resource.service.service.exception.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ResourceServiceImpl(
    private val metaRepository: ResourceMetaRepository,
    private val dataRepository: ResourceDataRepository,
) : ResourceService {

    override fun saveResource(filename: String, data: ByteArray): ResourceMeta {
        if (metaRepository.existsByFilename(filename)) {
            throw EntityDuplicateException("filename = $filename")
        }
        return dataRepository.upload(filename, data.inputStream(), data.size.toLong()).let(metaRepository::save)
    }

    override fun saveResource(filename: String, data: InputStream, size: Long): ResourceMeta {
        if (metaRepository.existsByFilename(filename)) {
            throw EntityDuplicateException("filename = $filename")
        }
        return dataRepository.upload(filename, data, size).let(metaRepository::save)
    }

    override fun findResource(id: Long): ResourceDTO {
        val meta = metaRepository.findByIdOrNull(id) ?: throw EntityNotFoundException("id = $id")
        val (data, size) = dataRepository.download(meta.filename)

        return ResourceDTO(
            filename = meta.filename,
            data = data,
            size = size,
        )
    }

    override fun deleteResources(ids: Iterable<Long>): Iterable<Long> {
        val resourceIdsByFilename =
            metaRepository.findAllById(ids).groupBy(
                ResourceMeta::filename,
                ResourceMeta::id,
            )
        val deletedResourceIds =
            dataRepository.delete(resourceIdsByFilename.keys)
                .mapNotNull(resourceIdsByFilename::get)
                .flatten()
                .filterNotNull()

        metaRepository.deleteAllById(deletedResourceIds)

        return deletedResourceIds
    }
}

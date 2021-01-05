package dev.domnikl.schema_registry_gitops

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException

class SchemaRegistryClient(private val client: CachedSchemaRegistryClient) {
    fun subjects(): List<String> {
        return client.allSubjects.toList()
    }

    fun globalCompatibility(): Compatibility {
        return Compatibility.valueOf(client.getCompatibility(""))
    }

    fun updateGlobalCompatibility(compatibility: Compatibility): Compatibility {
        return Compatibility.valueOf(client.updateCompatibility("", compatibility.toString()))
    }

    fun compatibility(subject: String): Compatibility {
        return handleNotExisting {
            Compatibility.valueOf(client.getCompatibility(subject))
        } ?: Compatibility.NONE
    }

    fun updateCompatibility(subject: Subject): Compatibility {
        return Compatibility.valueOf(client.updateCompatibility(subject.name, subject.compatibility.toString()))
    }

    fun testCompatibility(subject: Subject): Boolean {
        return client.testCompatibility(subject.name, subject.schema)
    }

    fun getLatestSchema(subject: String): ParsedSchema {
        val metadata = client.getLatestSchemaMetadata(subject)

        return client.parseSchema(metadata.schemaType, metadata.schema, metadata.references).get()
    }

    fun create(subject: Subject): Int {
        return client.register(subject.name, subject.schema)
    }

    fun evolve(subject: Subject): Int {
        return client.register(subject.name, subject.schema)
    }

    fun version(subject: Subject): Int? {
        return handleNotExisting {
            client.getVersion(subject.name, subject.schema)
        }
    }

    private fun <V> handleNotExisting(f: () -> V?): V? {
        return try {
            f()
        } catch (e: RestClientException) {
            when (e.errorCode) {
                ERROR_CODE_SUBJECT_NOT_FOUND -> null
                ERROR_CODE_VERSION_NOT_FOUND -> null
                ERROR_CODE_SCHEMA_NOT_FOUND -> null
                else -> throw e
            }
        }
    }

    companion object {
        private const val ERROR_CODE_SUBJECT_NOT_FOUND = 40401
        private const val ERROR_CODE_VERSION_NOT_FOUND = 40402
        private const val ERROR_CODE_SCHEMA_NOT_FOUND = 40403
    }
}

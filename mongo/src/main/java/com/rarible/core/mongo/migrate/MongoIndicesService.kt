package com.rarible.core.mongo.migrate

import com.mongodb.client.model.IndexOptions
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.stereotype.Service

@Service
class MongoIndicesService {
    @Deprecated("Do not use in new code. Use only to support quick migration to mongock")
    fun createIndices(template: MongoOperations, collection: String, indices: List<NewIndex>) {
        val indexOps = template.indexOps(collection)
        indices.forEach { index ->
            val def = index.key.entries.fold(Index()) {
                def, item -> def.on(item.key, if (item.value == 1) Sort.Direction.ASC else Sort.Direction.DESC)
            }
            if (index.options.isBackground) {
                def.background()
            }
            if (index.options.isSparse) {
                def.sparse()
            }
            if (index.options.isUnique) {
                def.unique()
            }
            if (index.options.partialFilterExpression != null) {
                def.partial(PartialIndexFilter.of(index.options.partialFilterExpression as Document))
            }
            indexOps.ensureIndex(def.named(index.name))
        }
    }
}

data class NewIndex(val name: String, val key: Document, val options: IndexOptions)
package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.Identifiable

interface EntityTemplateProvider<Id, E : Identifiable<Id>> {
    fun getEntityTemplate(id: Id, version: Long? = null): E
    fun getEntityTemplateFromEntity(entity: E, version: Long? = null): E
}

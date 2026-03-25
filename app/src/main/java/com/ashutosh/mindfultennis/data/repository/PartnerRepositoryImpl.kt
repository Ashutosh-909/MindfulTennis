package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.data.local.db.dao.PartnerDao
import com.ashutosh.mindfultennis.data.local.db.entity.SyncStatus
import com.ashutosh.mindfultennis.data.local.db.entity.toDomain
import com.ashutosh.mindfultennis.data.local.db.entity.toEntity
import com.ashutosh.mindfultennis.domain.model.Partner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartnerRepositoryImpl @Inject constructor(
    private val partnerDao: PartnerDao,
) : PartnerRepository {

    override fun observeAll(userId: String): Flow<List<Partner>> =
        partnerDao.observeAllForUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(userId: String): List<Partner> = withContext(Dispatchers.IO) {
        partnerDao.getAllForUser(userId).map { it.toDomain() }
    }

    override suspend fun getById(id: String): Partner? = withContext(Dispatchers.IO) {
        partnerDao.getById(id)?.toDomain()
    }

    override suspend fun create(partner: Partner): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = partner.toEntity(SyncStatus.PENDING)
            partnerDao.upsert(entity)
        }
    }

    override suspend fun delete(partnerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            partnerDao.updateSyncStatus(partnerId, SyncStatus.PENDING_DELETE.name)
        }
    }

    override suspend fun deleteAllForUser(userId: String) = withContext(Dispatchers.IO) {
        partnerDao.deleteAllForUser(userId)
    }
}

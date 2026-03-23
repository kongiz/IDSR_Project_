package com.idsr_project.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingReportDao {

    @Insert
    suspend fun insert(report: PendingReportEntity): Long

    @Query("SELECT * FROM pending_reports WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingReports(): List<PendingReportEntity>

    @Query("SELECT * FROM pending_reports ORDER BY createdAt DESC")
    suspend fun getAllReports(): List<PendingReportEntity>

    @Query("SELECT * FROM pending_reports WHERE submittedBy = :username ORDER BY createdAt DESC")
    suspend fun getReportsByUser(username: String): List<PendingReportEntity>

    @Query("SELECT * FROM pending_reports WHERE regionName = :region ORDER BY createdAt DESC")
    suspend fun getReportsByRegion(region: String): List<PendingReportEntity>

    @Query("SELECT * FROM pending_reports WHERE districtName = :district ORDER BY createdAt DESC")
    suspend fun getReportsByDistrict(district: String): List<PendingReportEntity>

    @Query("UPDATE pending_reports SET status = :status, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM pending_reports WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM pending_reports WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingAndFailedReports(): List<PendingReportEntity>

    @Query("DELETE FROM pending_reports WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_reports WHERE status = 'FAILED'")
    suspend fun deleteAllFailed()

    @Query("""
    DELETE FROM pending_reports 
    WHERE status = 'SYNCED' 
    AND createdAt < :cutoffTimestamp""")
    suspend fun deleteOldSyncedReports(cutoffTimestamp: Long): Int

}
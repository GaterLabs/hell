package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        RouteEntity::class,
        StoreEntity::class,
        StoreConsignmentEntity::class,
        VanLoadEntity::class,
        InventoryMovementEntity::class,
        DailyClosingEntity::class,
        DebtWriteOffEntity::class,
        BusinessPartnerEntity::class,
        StorePriceOverrideEntity::class,
        AuditEventEntity::class,
        VanReturnEntity::class,
        VisitTransactionEntity::class,
        TransactionItemEntity::class,
        GpsTrackEntity::class,
        GpsSessionEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun routeDao(): RouteDao
    abstract fun storeDao(): StoreDao
    abstract fun consignmentDao(): StoreConsignmentDao
    abstract fun vanLoadDao(): VanLoadDao
    abstract fun inventoryMovementDao(): InventoryMovementDao
    abstract fun dailyClosingDao(): DailyClosingDao
    abstract fun debtWriteOffDao(): DebtWriteOffDao
    abstract fun businessPartnerDao(): BusinessPartnerDao
    abstract fun storePriceOverrideDao(): StorePriceOverrideDao
    abstract fun auditEventDao(): AuditEventDao
    abstract fun vanReturnDao(): VanReturnDao
    abstract fun transactionDao(): TransactionDao
    abstract fun gpsTrackDao(): GpsTrackDao
    abstract fun gpsSessionDao(): GpsSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salestrack_prod.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS inventory_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        bucket TEXT NOT NULL,
                        quantityPcs INTEGER NOT NULL,
                        movementType TEXT NOT NULL,
                        referenceId INTEGER,
                        unitCostPerPc REAL NOT NULL,
                        notes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE CASCADE
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_productId ON inventory_movements(productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_bucket ON inventory_movements(bucket)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_createdAt ON inventory_movements(createdAt)")
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS daily_closings (
                        dateString TEXT NOT NULL PRIMARY KEY,
                        totalLoadedBoxes INTEGER NOT NULL,
                        freshRemainingBoxes INTEGER NOT NULL,
                        factoryDue REAL NOT NULL,
                        cashCollected REAL NOT NULL,
                        shortage REAL NOT NULL,
                        closedAt INTEGER NOT NULL,
                        notes TEXT NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_closings_closedAt ON daily_closings(closedAt)")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transaction_items ADD COLUMN sourceBucket TEXT NOT NULL DEFAULT 'FRESH_FACTORY'")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stores ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE stores ADD COLUMN creditLimit REAL NOT NULL DEFAULT 500000.0")
                db.execSQL("ALTER TABLE stores ADD COLUMN debtSince INTEGER")
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS debt_write_offs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        storeId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        reason TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(storeId) REFERENCES stores(id) ON DELETE RESTRICT
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_write_offs_storeId ON debt_write_offs(storeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_write_offs_createdAt ON debt_write_offs(createdAt)")
            }
        }

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS business_partners (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    kind TEXT NOT NULL,
                    name TEXT NOT NULL,
                    contactName TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    address TEXT NOT NULL,
                    paymentTerms TEXT NOT NULL,
                    bankAccount TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    active INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_business_partners_kind ON business_partners(kind)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_business_partners_active ON business_partners(active)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS store_price_overrides (
                    storeId INTEGER NOT NULL,
                    productId INTEGER NOT NULL,
                    pricePerPc REAL NOT NULL,
                    validFrom TEXT NOT NULL,
                    validUntil TEXT,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(storeId, productId)
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_store_price_overrides_storeId ON store_price_overrides(storeId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_store_price_overrides_productId ON store_price_overrides(productId)")
            }
        }

        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS audit_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    eventType TEXT NOT NULL,
                    referenceId INTEGER,
                    description TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_createdAt ON audit_events(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_eventType ON audit_events(eventType)")
            }
        }

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visit_transactions ADD COLUMN visitLatitude REAL")
                db.execSQL("ALTER TABLE visit_transactions ADD COLUMN visitLongitude REAL")
                db.execSQL("ALTER TABLE visit_transactions ADD COLUMN gpsAccuracyMeters REAL")
                db.execSQL("ALTER TABLE visit_transactions ADD COLUMN gpsDistanceMeters REAL")
            }
        }

        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stores ADD COLUMN photoUri TEXT")
            }
        }
    }
}

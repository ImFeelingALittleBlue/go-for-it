package com.example.goforit.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object MapBuildRepository {
    private val records = mutableStateListOf<MapBuildRecord>()
    private var started = false

    fun records(): SnapshotStateList<MapBuildRecord> = records

    fun start() {
        if (started) return
        started = true
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnSuccessListener { listen() }
        } else {
            listen()
        }
    }

    private fun listen() {
        collection()?.orderBy("builtAt", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                records.clear()
                for (doc in snapshot.documents) {
                    doc.toObject(MapBuildRecord::class.java)
                        ?.copy(docId = doc.id)
                        ?.let { records.add(it) }
                }
            }
    }

    fun add(heritage: Heritage) {
        if (isBuilt(heritage.id)) return
        val data = hashMapOf(
            "heritageId" to heritage.id,
            "name" to heritage.name,
            "builtAt" to System.currentTimeMillis()
        )
        collection()?.add(data)
    }

    fun isBuilt(heritageId: Int): Boolean =
        records.any { it.heritageId == heritageId }

    private fun collection(): CollectionReference? {
        val uid = Firebase.auth.currentUser?.uid ?: return null
        return Firebase.firestore
            .collection("users").document(uid)
            .collection("map_builds")
    }
}

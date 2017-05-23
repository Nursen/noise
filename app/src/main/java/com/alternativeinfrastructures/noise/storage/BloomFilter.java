package com.alternativeinfrastructures.noise.storage;

import android.os.Looper;
import android.util.Log;

import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import util.hash.MurmurHash3;

// Actual bloom filter implementation based heavily on this guide:
// http://blog.michaelschmatz.com/2016/04/11/how-to-write-a-bloom-filter-cpp/
@Table(database = MessageDatabase.class)
public class BloomFilter extends BaseModel {
    public static final String TAG = "BloomFilter";

    // TODO: Tune these
    // They need to be large enough to describe billions of messages
    // but also small enough to transmit in a few seconds over Bluetooth
    static final int SIZE = 1 << 20; // in bits
    static final int NUM_HASHES = 5;

    @PrimaryKey
    @ForeignKey(onDelete = ForeignKeyAction.CASCADE, stubbedRelationship = true)
    UnknownMessage message;

    @PrimaryKey
    int hash;

    BloomFilter() {}

    static List<Integer> hashMessage(UnknownMessage message) {
        Vector<Integer> hashList = new Vector<Integer>();

        MurmurHash3.LongPair primaryHash = new MurmurHash3.LongPair();
        MurmurHash3.murmurhash3_x64_128(message.payload.getBlob(), 0 /*offset*/, UnknownMessage.PAYLOAD_SIZE, 0 /*seed*/, primaryHash);
        for (int hashFunction = 0; hashFunction < NUM_HASHES; ++hashFunction)
            hashList.add((int) nthHash(primaryHash.val1, primaryHash.val2, hashFunction));

        return hashList;
    }

    static void addMessage(UnknownMessage message) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread())
            Log.e(TAG, "Attempting to save on the UI thread");

        for (int hash : hashMessage(message)) {
            BloomFilter row = new BloomFilter();
            row.message = message;
            row.hash = hash;
            row.save();
        }
    }

    public static BitSet getMessageVector() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread())
            Log.e(TAG, "Attempting to get the bit vector on the UI thread");

        // TODO: Make this query async so we don't block the caller
        List<BloomFilter> hashList = SQLite.select(BloomFilter_Table.hash)
                .from(BloomFilter.class)
                .orderBy(BloomFilter_Table.hash, true /*ascending*/)
                .queryList();

        BitSet messageVector = new BitSet(SIZE);
        for (BloomFilter hash : hashList) {
            messageVector.set(hash.hash);
        }

        return messageVector;
    }

    private static long nthHash(long hashA, long hashB, int hashFunction) {
        // Double modulus ensures that the result is positive when any of the hashes are negative
        return ((hashA + hashFunction * hashB) % SIZE + SIZE) % SIZE;
    }

    // TODO: Write a query that gets an UnknownMessage using its hash values
}

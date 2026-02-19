package com.maachang.mrag.vector;

import java.util.*;
import java.util.concurrent.*;

/**
 * ベクターストアー管理オブジェクト.
 */
public class VectorStore {

    // ベクターストアーファイル格納パス.
    private String path;

    // vectorStore管理下のvectorGroupを管理するオブジェクト.
    private Map<String, VectorGroup> stores =
        new ConcurrentHashMap<String, VectorGroup>();
    
    


}
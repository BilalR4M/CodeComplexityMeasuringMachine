//package com.example.wcc;
//
//import static com.mongodb.client.model.Filters.eq;
//import org.bson.Document;
//import com.mongodb.client.MongoClient;
//import com.mongodb.client.MongoClients;
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoDatabase;
//
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//
//@RestController
//@CrossOrigin
//public class DatabaseConnection {
//
//    @PostMapping("/calculate-maintainability-index")
//    public String getDatabase() {
//        String uri = "mongodb+srv://it22219916:200209401591@cluster0.m241yfi.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
//        try (MongoClient mongoClient = MongoClients.create(uri)) {
//            MongoDatabase database = mongoClient.getDatabase("sample_mflix");
//            MongoCollection<Document> collection = database.getCollection("movies");
//            Document doc = collection.find(eq("title", "Back to the Future")).first();
//            if (doc != null) {
//                System.out.println(doc.toJson());
//            } else {
//                System.out.println("No matching documents found.");
//            }
//        }
//        return uri;
//    }
//
//}

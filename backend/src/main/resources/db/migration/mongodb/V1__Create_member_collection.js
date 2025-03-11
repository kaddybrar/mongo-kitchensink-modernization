// Create member collection with schema validation
db.createCollection("member", {
   validator: {
      $jsonSchema: {
         bsonType: "object",
         required: ["name", "email"],
         properties: {
            name: {
               bsonType: "string",
               description: "must be a string and is required"
            },
            email: {
               bsonType: "string",
               pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
               description: "must be a valid email address and is required"
            },
            phoneNumber: {
               bsonType: "string",
               pattern: "^\\+[1-9]\\d{1,14}$",
               description: "must be a valid phone number starting with +"
            },
            createdAt: {
               bsonType: "date",
               description: "must be a date"
            },
            updatedAt: {
               bsonType: "date",
               description: "must be a date"
            }
         }
      }
   }
});

// Create indexes
db.member.createIndex({ "email": 1 }, { unique: true });
db.member.createIndex({ "name": 1 }); 
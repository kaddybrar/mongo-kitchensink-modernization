// Check if collection exists and is empty
if (!db.getCollectionNames().includes("member") || db.member.countDocuments() === 0) {
    print("Collection is empty or does not exist, inserting sample data...");
    db.member.insertMany([
        {
            name: "John Doe",
            email: "john.doe@example.com",
            phoneNumber: "+11234567890",
            createdAt: new Date(),
            updatedAt: new Date()
        },
        {
            name: "Jane Smith",
            email: "jane.smith@example.com",
            phoneNumber: "+19876543210",
            createdAt: new Date(),
            updatedAt: new Date()
        },
        {
            name: "Bob Johnson",
            email: "bob.johnson@example.com",
            phoneNumber: "+15551234567",
            createdAt: new Date(),
            updatedAt: new Date()
        }
    ]);
    print("Sample data inserted successfully");
} else {
    print("Collection already contains data, skipping sample data insertion");
} 
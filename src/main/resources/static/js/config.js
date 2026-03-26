// Environment Configuration - Sensitive Information for Testing Purposes
const config = {
    // AWS Credentials (Dummy for testing)
    aws: {
        accessKeyId: "AKIAIOSFODNN7EXAMPLE",
        secretAccessKey: "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
        region: "us-east-1"
    },

    // Firebase Project Config
    firebase: {
        apiKey: "AIzaSyAs-D4XyR6s8uV-Xv9J5K8_example_key",
        authDomain: "test-project-12345.firebaseapp.com",
        projectId: "test-project-12345",
        storageBucket: "test-project-12345.appspot.com",
        messagingSenderId: "123456789012",
        appId: "1:123456789012:web:abcdef1234567890"
    },

    // Google Maps API Key
    googleMaps: {
        apiKey: "AIzaSyAs-D4XyR6s8uV-Xv9J5K8_example_google_key"
    },

    // Database Connection Strings (Internal)
    database: {
        host: "db.internal.example.com",
        port: 5432,
        user: "admin_user",
        password: "SuperSecretPassword123!",
        database: "production_db"
    },

    // Stripe Secret Key
    stripe: {
        secretKey: "sk_test_4eC39HqLyjWDarjtT1zdp7dc"
    },

    // GitHub Personal Access Token
    github: {
        token: "ghp_nZ8qV2m4K5L6N7M8O9P0Q1R2S3T4U5V6W7X8"
    }
};

console.log("Configuration loaded successfully.");
export default config;

# DataFlowHubLibraryJava
project to practice Java without frameworks

```mermaid

classDiagram
    class User {
        UUID id
        String name
        String email
        UserRole role
        Instant createdAt
    }
    class DataFile {
        UUID id
        String originalFilename
        String storagePath
        long sizeBytes
        String checksumSha256
        Instant uploadedAt
    }
    class ProcessingRequest {
        UUID id
        String title
        Map<String,String> parameters
        RequestStatus status
        Instant createdAt
    }
    class BatchJobConfig {
        UUID id
        String name
        String description
        int chunkSize
        ReaderType readerType
        WriterType writerType
        boolean allowRestart
        Instant createdAt
        boolean active
    }
    class JobExecution {
        UUID id
        Instant startTime
        Instant endTime
        ExecutionStatus exitStatus
        long readCount
        long writeCount
        long skipCount
        String errorMessage
    }
    class Report {
        UUID id
        Instant generatedAt
        String filePath
        String summary
    }

    User "1" -- "*" DataFile : uploadedBy
    User "1" -- "*" ProcessingRequest : requestedBy
    DataFile "1" -- "*" ProcessingRequest : uses
    ProcessingRequest "1" -- "1" BatchJobConfig : config
    ProcessingRequest "1" -- "*" JobExecution : has
    ProcessingRequest "1" -- "1" Report : generates
    JobExecution "0..1" -- "1" Report : produces
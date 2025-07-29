# DataFlowHubLibraryJava
project to practice Java without frameworks

classDiagram
  class User {
    +UUID id
    +String name
    +String email
  }
  class Transaction {
    +UUID id
    +BigDecimal amount
    +LocalDateTime timestamp
    +UUID userId
  }
  User "1" --> "many" Transaction
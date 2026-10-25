# Campus Resource Management System

Simple Java console project for CSC2104 OOP.

## Run the full app

Requires Java 17+, Node.js, and npm. Use two terminals from the project root.

**Terminal 1 — Java API**

```powershell
javac -d out src\campus\*.java
java -cp out campus.ApiServer
```

**Terminal 2 — Vite + React UI**

```powershell
cd campus-ui
npm install
npm run dev
```

Open the local URL printed by Vite. Its development server proxies `/api` to `http://localhost:8080`.

**Architecture**

- `src/campus/` contains Java domain classes, resource rules, booking logic, and file persistence.
- `ApiServer` exposes the Java manager and storage operations over HTTP on port 8080.
- `campus-ui/` is a Vite + React client; `src/api.js` owns its HTTP calls.
- React Three Fiber renders the resources; panels and animations present API-backed interactions.
- Vite proxies `/api` during development; `npm run build` creates the production UI bundle.

## Current features

- Java `Role` and `ResourceType` enums plus a shared abstract `BookableResource` base.
- ID-based equality for users/resources and synchronized booking/cancellation rules.
- Ownership-protected cancellation: only the booking owner or an Admin can cancel.
- Resource details (room capacity, lab equipment, and book author) in the API and UI.
- Timestamped booking, cancellation, and rejected-operation activity logs.
- Optional ISO-8601 time slots with overlap checks; books receive a seven-day loan period.
- Overdue book fines are calculated at ₹5/day for Students, ₹2/day for Faculty, and ₹0/day for Admins.
- Legacy five-column booking files remain readable; new saves append slot/loan fields.

## Tests

Production Java sources compile with:

```powershell
javac -d out src\campus\*.java
```

JUnit 5 tests are in `test/campus/ResourceManagerTest.java`. The repository does not vendor a JUnit runner or dependency manager; run them with a JUnit 5 console standalone jar on the classpath, for example:

```powershell
javac -cp junit-platform-console-standalone.jar -d out src\campus\*.java test\campus\ResourceManagerTest.java
java -jar junit-platform-console-standalone.jar --class-path out --scan-class-path
```

The frontend production bundle is built with `npm.cmd run build` from `campus-ui/`.

Choose `1` to list resources, `2` to search, `3` to book, `4` to cancel, and `6` to save data in `data/`.

Option `8` demonstrates synchronized booking with two threads.

Demo users: `S1001`, `S1002`, `F2001`, `A3001`.

## OOP concepts used

- **Class and object:** A class is a blueprint; objects such as `Room`, `Book`, and `Booking` are created from it.
- **Encapsulation:** Data is kept private and accessed through methods, so each class controls valid changes to its state.
- **Abstraction:** `User` and `Resource` provide essential shared behaviour while hiding role- or resource-specific details.
- **Inheritance:** `Student`, `Faculty`, and `Admin` inherit from `User`; `Room` and `Book` inherit from `Resource`; `Lab` extends `Room`.
- **Polymorphism:** A `User` or `Resource` reference can use the correct overridden method for its actual subtype at runtime.
- **Method overriding:** Subclasses redefine methods such as `canBook`, `getBookingLimit`, and `toString` for their own rules.
- **Interface:** `Bookable` and `Searchable` define contracts that different classes can implement.
- **Composition:** `University`, `ResourceManager`, and `Main` work by holding and coordinating other objects.
- **Exception handling:** Custom checked exceptions communicate invalid users, missing bookings, and unavailable resources.
- **Static members:** `Booking.nextId` is shared by all booking objects to generate IDs.
- **Inner class:** `Booking.Details` belongs to a booking and can read its enclosing booking data.
- **Collections and generics:** `List`, `Map`, and `ConcurrentHashMap` store typed groups of resources, users, and bookings.
- **Recursion:** `ResourceManager.recursiveSearch` searches a resource list by calling itself with the next index.
- **Concurrency:** `synchronized`, `wait`, and `notifyAll` protect booking changes when multiple threads try to book.
- **Persistence:** `StorageService` writes objects as text records and recreates them when the program starts.

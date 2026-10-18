# UML diagrams

The diagrams below are written in [Mermaid](https://mermaid.js.org/), so they live as text next to the code and are
rendered by GitHub and by the IntelliJ Markdown preview (Mermaid support may need to be enabled in
*Settings → Languages & Frameworks → Markdown*).

1. [Class diagram](#1-class-diagram)
2. [DTOs and exceptions](#2-dtos-and-exceptions)
3. [Order lifecycle](#3-order-lifecycle)
4. [Sequence: ordering, preparing and delivering pancakes](#4-sequence-ordering-preparing-and-delivering-pancakes)
5. [Sequence: rejected operation](#5-sequence-rejected-operation)
6. [Sequence: concurrent deliveries](#6-sequence-concurrent-deliveries)

## 1. Class diagram

`PancakeService` is the public API. It only coordinates: it looks orders up in the `OrderRepository`, delegates the
rules to the domain (`model` package) and returns ids and immutable records, never domain objects.

```mermaid
classDiagram
    direction LR

    class PancakeService {
        +PancakeService(Set~String~ menuIngredients, Set~Integer~ buildings)
        +PancakeService(Set~String~ menuIngredients, Set~Integer~ buildings, OrderLog log)
        +createOrder(int building, int room) UUID
        +addPancake(UUID orderId) UUID
        +addIngredient(UUID orderId, UUID pancakeId, String ingredient)
        +removePancake(UUID orderId, UUID pancakeId)
        +viewOrder(UUID orderId) List~PancakeDetails~
        +completeOrder(UUID orderId)
        +listCompletedOrders() Set~UUID~
        +prepareOrder(UUID orderId)
        +listPreparedOrders() Set~UUID~
        +deliverOrder(UUID orderId) DeliveryDetails
        +cancelOrder(UUID orderId)
    }

    class Order {
        -UUID id
        -DeliveryAddress address
        -Map~UUID,Pancake~ pancakes
        -OrderStatus status
        +Order(DeliveryAddress address)
        +getId() UUID
        +getAddress() DeliveryAddress
        +getStatus() OrderStatus
        +addPancake(Pancake pancake) UUID
        +addIngredient(UUID pancakeId, Ingredient ingredient)
        +removePancake(UUID pancakeId)
        +getPancakes() List~PancakeDetails~
        +complete()
        +prepare()
        +deliver()
        +cancel()
    }

    class Pancake {
        -List~Ingredient~ ingredients
        +plain()$ Pancake
        +with(Ingredient ingredient) Pancake
        +description() String
    }

    class Ingredient {
        <<record>>
        String name
    }

    class DeliveryAddress {
        <<record>>
        int building
        int room
    }

    class OrderStatus {
        <<enumeration>>
        CREATED
        COMPLETED
        PREPARED
        DELIVERED
        CANCELLED
    }

    class Menu {
        +Menu(Set~String~ ingredients)
        +ingredient(String name) Ingredient
    }

    class BuildingRegistry {
        +BuildingRegistry(Set~Integer~ buildings)
        +addressFor(int building, int room) DeliveryAddress
    }

    class OrderRepository {
        +save(Order order)
        +find(UUID orderId) Order
        +findByStatus(OrderStatus status) List~Order~
        +remove(UUID orderId)
    }

    class OrderLog {
        <<interface>>
        +log(String message)
    }

    class InMemoryOrderLog {
        +log(String message)
        +entries() List~String~
    }

    PancakeService --> Menu
    PancakeService --> BuildingRegistry
    PancakeService --> OrderRepository
    PancakeService --> OrderLog
    OrderLog <|.. InMemoryOrderLog
    OrderRepository o-- "*" Order
    Order *-- "*" Pancake
    Order --> DeliveryAddress
    Order --> OrderStatus
    Pancake o-- "*" Ingredient
    Menu ..> Ingredient : creates
    BuildingRegistry ..> DeliveryAddress : creates
```

- **Composition** (`*--`): an order owns its pancakes, which do not exist outside it. Each pancake is identified inside
  the order by the id returned from `addPancake`.
- **Aggregation** (`o--`): the repository stores orders, and a pancake refers to ingredients that exist on their own.
- **Dependency** (`..>`): `Menu` and `BuildingRegistry` validate the client's input and create the value objects.
- The menu ingredients and the registered buildings are supplied by the API client through the `PancakeService`
  constructor, so nothing about the Dojo is hard-coded in the library.
- **Thread safety**:
  - `Order`: every method that reads or changes its pancakes or status is `synchronized`.
  - `Pancake`: immutable; `with()` returns a new pancake.
  - `OrderRepository`: backed by a `ConcurrentHashMap`; `remove()` checks and removes atomically.
  - `InMemoryOrderLog`: backed by a `CopyOnWriteArrayList`; `entries()` returns an immutable copy.

## 2. DTOs and exceptions

The API only returns ids and the immutable records below. Validation and business-rule failures are reported with
unchecked exceptions that share the abstract `PancakeLabException` base class and expose the rejected value.

```mermaid
classDiagram
    direction TB

    class PancakeDetails {
        <<record>>
        UUID id
        String description
    }

    class DeliveryDetails {
        <<record>>
        UUID orderId
        int building
        int room
        List~String~ pancakes
    }

    class RuntimeException
    class PancakeLabException {
        <<abstract>>
    }
    class UnknownBuildingException {
        +getBuilding() int
    }
    class InvalidRoomException {
        +getRoom() int
    }
    class UnknownIngredientException {
        +getIngredient() String
    }
    class DuplicateIngredientException {
        +getIngredient() String
    }
    class OrderNotFoundException {
        +getOrderId() UUID
    }
    class PancakeNotFoundException {
        +getPancakeId() UUID
    }
    class EmptyOrderException {
        +getOrderId() UUID
    }
    class InvalidOrderStateException {
        +getOrderId() UUID
        +getStatus() String
    }

    RuntimeException <|-- PancakeLabException
    PancakeLabException <|-- UnknownBuildingException
    PancakeLabException <|-- InvalidRoomException
    PancakeLabException <|-- UnknownIngredientException
    PancakeLabException <|-- DuplicateIngredientException
    PancakeLabException <|-- OrderNotFoundException
    PancakeLabException <|-- PancakeNotFoundException
    PancakeLabException <|-- EmptyOrderException
    PancakeLabException <|-- InvalidOrderStateException
```

| Exception                      | Thrown by                       | When                                                     |
|--------------------------------|---------------------------------|----------------------------------------------------------|
| `UnknownBuildingException`     | `BuildingRegistry`              | the building is not registered                           |
| `InvalidRoomException`         | `DeliveryAddress`               | the room number is not positive                          |
| `UnknownIngredientException`   | `Menu`                          | the ingredient is not on the menu, including `null`      |
| `DuplicateIngredientException` | `Pancake`                       | the ingredient is already on the pancake                 |
| `OrderNotFoundException`       | `OrderRepository`               | the order id is unknown or `null`                        |
| `PancakeNotFoundException`     | `Order`                         | the pancake id is unknown or `null`                      |
| `EmptyOrderException`          | `Order`                         | completing an order without pancakes                     |
| `InvalidOrderStateException`   | `Order`                         | the operation is not allowed in the current order status |

## 3. Order lifecycle

```mermaid
stateDiagram-v2
    [*] --> CREATED: createOrder
    CREATED --> CREATED: addPancake / addIngredient / removePancake
    CREATED --> COMPLETED: completeOrder (needs a pancake)
    CREATED --> CANCELLED: cancelOrder
    COMPLETED --> PREPARED: prepareOrder (Chef)
    COMPLETED --> CANCELLED: cancelOrder
    PREPARED --> DELIVERED: deliverOrder (Delivery)
    DELIVERED --> [*]: removed from the repository
    CANCELLED --> [*]: removed from the repository

    note right of CREATED
        The only status in which pancakes
        and ingredients can be changed
    end note
```

Transitions are strict: any operation attempted from a status that does not allow it, including repeating the
operation that led to the current status, throws `InvalidOrderStateException`.

## 4. Sequence: ordering, preparing and delivering pancakes

The main use case of the assignment, with the Disciple, the Chef and the Delivery service.

```mermaid
sequenceDiagram
    actor Disciple
    actor Chef
    actor Delivery
    participant Service as PancakeService
    participant Registry as BuildingRegistry
    participant Menu
    participant Repo as OrderRepository
    participant Order
    participant Log as OrderLog

    Disciple->>Service: createOrder(10, 5)
    Service->>Registry: addressFor(10, 5)
    Registry-->>Service: DeliveryAddress
    Service->>Repo: save(new Order(address))
    Service-->>Disciple: orderId

    Disciple->>Service: addPancake(orderId)
    Service->>Repo: find(orderId)
    Repo-->>Service: Order
    Service->>Order: addPancake(Pancake.plain())
    Order-->>Service: pancakeId
    Service->>Log: log("Added pancake ...")
    Service-->>Disciple: pancakeId

    Disciple->>Service: addIngredient(orderId, pancakeId, "dark chocolate")
    Service->>Repo: find(orderId)
    Repo-->>Service: Order
    Service->>Menu: ingredient("dark chocolate")
    Menu-->>Service: Ingredient
    Service->>Order: addIngredient(pancakeId, ingredient)
    Service->>Log: log("Added ingredient ...")

    Disciple->>Service: completeOrder(orderId)
    Service->>Repo: find(orderId)
    Repo-->>Service: Order
    Service->>Order: complete()

    Chef->>Service: listCompletedOrders()
    Service->>Repo: findByStatus(COMPLETED)
    Repo-->>Service: List of Order
    Service-->>Chef: Set of order ids
    Chef->>Service: prepareOrder(orderId)
    Service->>Repo: find(orderId)
    Repo-->>Service: Order
    Service->>Order: prepare()

    Delivery->>Service: listPreparedOrders()
    Service->>Repo: findByStatus(PREPARED)
    Repo-->>Service: List of Order
    Service-->>Delivery: Set of order ids
    Delivery->>Service: deliverOrder(orderId)
    Service->>Repo: find(orderId)
    Repo-->>Service: Order
    Service->>Order: deliver()
    Service->>Repo: remove(orderId)
    Service->>Order: getPancakes()
    Order-->>Service: List of PancakeDetails
    Service->>Log: log("Order ... out for delivery.")
    Service-->>Delivery: DeliveryDetails
```

Cancelling follows the same pattern as delivering: `find`, then `cancel()` on the order, then `remove` and `log`. The
order is removed only after the state transition succeeds, so a rejected cancellation or delivery keeps the order.

## 5. Sequence: rejected operation

Every validation happens before the order is changed, so a rejected operation leaves the order untouched and logs
nothing.

```mermaid
sequenceDiagram
    actor Villain as Dr. Fu Man Chu
    participant Service as PancakeService
    participant Repo as OrderRepository
    participant Menu
    participant Order

    Villain->>Service: addIngredient(orderId, pancakeId, "mustard")
    Service->>Repo: find(orderId)
    Repo-->>Service: Order
    Service->>Menu: ingredient("mustard")
    Menu--xService: throws UnknownIngredientException
    Service--xVillain: UnknownIngredientException (getIngredient() = "mustard")
    Note over Order: not changed, nothing is logged
```

## 6. Sequence: concurrent deliveries

Two delivery threads try to deliver the same prepared order at the same time. The order lock makes the status check
and change a single step, and the atomic removal in the repository is a second line of defence, so only one delivery
succeeds.

```mermaid
sequenceDiagram
    participant D1 as Delivery thread 1
    participant D2 as Delivery thread 2
    participant Service as PancakeService
    participant Order

    par
        D1->>Service: deliverOrder(orderId)
    and
        D2->>Service: deliverOrder(orderId)
    end
    Service->>Order: deliver() for thread 1, which gets the lock
    Note over Order: status becomes DELIVERED
    Service->>Order: deliver() for thread 2, after waiting for the lock
    Order--xService: throws InvalidOrderStateException (DELIVERED)
    Service-->>D1: DeliveryDetails
    Service--xD2: InvalidOrderStateException
    Note over D2: or OrderNotFoundException, if the order was already removed
```

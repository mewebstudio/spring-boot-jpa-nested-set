# Nested Set Tree Utilities for Spring Boot JPA

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven badge](https://maven-badges.herokuapp.com/maven-central/com.mewebstudio/spring-boot-jpa-nested-set/badge.svg?style=flat)](https://central.sonatype.com/artifact/com.mewebstudio/spring-boot-jpa-nested-set)
[![javadoc](https://javadoc.io/badge2/com.mewebstudio/spring-boot-jpa-nested-set/javadoc.svg)](https://javadoc.io/doc/com.mewebstudio/spring-boot-jpa-nested-set)

This package provides a generic and reusable implementation of the [Nested Set Model](https://en.wikipedia.org/wiki/Nested_set_model) for managing hierarchical data using Spring Boot and JPA.

It is designed to be extended and adapted for any entity that implements the `INestedSetNode<ID, T extends INestedSetNode<ID, T>>` interface.

---

## 📦 Package Overview

**Package:** `com.mewebstudio.springboot.jpa.nestedset`

### Core Components

- **`INestedSetNode<ID, T extends INestedSetNode<ID, T>>`**  
  Interface that defines the structure of a nested set node (left, right, parent).

- **`INestedSetNodeResponse<ID>`**  
  Interface for representing nodes with children, used for building hierarchical responses.

- **`JpaNestedSetRepository<T extends INestedSetNode<ID, T>, ID> extends JpaRepository<T, ID>`**  
  Base JPA repository interface with custom queries for nested set operations (e.g. find ancestors, descendants, siblings).

- **`AbstractNestedSetService<T extends INestedSetNode<ID, T>, ID>`**  
  Abstract service class that implements common logic for creating, moving, and restructuring nodes in a nested set tree.

---

## ✅ Features

- Create new nodes in the correct position in the nested set.
- Move nodes up or down among siblings.
- Retrieve ancestors and descendants of a node.
- Rebuild the entire tree from an unordered list of nodes.
- Shift and close gaps in the tree on node deletion.
- Generic and type-safe structure, reusable across multiple domain entities.

---

## 📥 Installation

#### for maven users
Add the following dependency to your `pom.xml` file:
```xml
<dependency>
  <groupId>com.mewebstudio</groupId>
  <artifactId>spring-boot-jpa-nested-set</artifactId>
  <version>0.1.2</version>
</dependency>
```
#### for gradle users
Add the following dependency to your `build.gradle` file:
```groovy
implementation 'com.mewebstudio:spring-boot-jpa-nested-set:0.1.2'
```

## 🚀 Usage

### 1. Example entity class `INestedSetNode<ID, T extends INestedSetNode<ID, T>>`
```java
@Entity
public class Category extends AbstractBaseEntity implements INestedSetNode<String, Category> {
    // implement getId, getLeft, getRight, getParent, etc.
}
```

### 2. Example repository interface `JpaNestedSetRepository<T extends INestedSetNode<ID, T>, ID> extends JpaRepository<T, ID>`
```java
public interface CategoryRepository extends JpaNestedSetRepository<Category, String> {
}
```

### 3. Example service class `AbstractNestedSetService<T extends INestedSetNode<ID, T>, ID>`
```java
// Example service class
@Service
public class CategoryService extends AbstractNestedSetService<Category, String> {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        super(categoryRepository);
        this.categoryRepository = categoryRepository;
    }

    // ...
}
```

## 🔁 Other Implementations

[Spring Boot JPA Nested Set (Kotlin Maven Package)](https://github.com/mewebstudio/spring-boot-jpa-nested-set-kotlin)

## 💡 Example Implementations

[Spring Boot JPA Nested Set - Java Implementation](https://github.com/mewebstudio/spring-boot-jpa-nested-set-java-impl)

[Spring Boot JPA Nested Set - Kotlin Implementation](https://github.com/mewebstudio/spring-boot-jpa-nested-set-kotlin-impl)

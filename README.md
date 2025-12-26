<h1 align="center"><strong>JPA Adapter for AG Grid Server-Side Mode</strong></h1>
<div align="center">
  <a href="https://central.sonatype.com/artifact/io.github.smolcan/ag-grid-jpa-adapter" target="_blank"><img src="https://img.shields.io/maven-central/v/io.github.smolcan/ag-grid-jpa-adapter?strategy=highestVersion&style=flat" alt="Version"></a>
  <a href="https://github.com/smolcan/ag-grid-jpa-adapter?tab=MIT-1-ov-file#readme" target="_blank"><img src="https://img.shields.io/github/license/smolcan/ag-grid-jpa-adapter" alt="License"></a>
  <a href="https://github.com/smolcan/ag-grid-jpa-adapter/graphs/contributors" target="_blank"><img src="https://img.shields.io/github/contributors/smolcan/ag-grid-jpa-adapter?style=flat" alt="Contributors"></a>
  <a href="https://github.com/smolcan/ag-grid-jpa-adapter/stargazers" target="_blank"><img src="https://img.shields.io/github/stars/smolcan/ag-grid-jpa-adapter?style=flat" alt="Stars"></a>
</div>

<p align="center">
   <a href="https://smolcan.github.io/ag-grid-jpa-adapter/" target="_blank"><strong>Explore Docs >></strong></a>
</p>
<p align="center">
   <a href="https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend" target="_blank"><strong>Showcase >></strong></a>
</p>

<p align="center">
A lightweight Java Maven library for integrating
<strong><a href="https://ag-grid.com/angular-data-grid/server-side-model/" target="_blank">AG Grid Server-Side Mode</a></strong>
with backend applications using <strong>JPA</strong>.
</p>

## Installation
Add the dependency to your `pom.xml` (Maven):
```xml
<dependency>
    <groupId>io.github.smolcan</groupId>
    <artifactId>ag-grid-jpa-adapter</artifactId>
    <version>${ag-grid-jpa-adapter.version}</version>
</dependency>
```

Or for Gradle:
```groovy
implementation 'io.github.smolcan:ag-grid-jpa-adapter:${agGridJpaAdapterVersion}'
```

## Quick Start
Create a `QueryBuilder` instance and delegate AG Grid requests to it.

Example usage in a Spring service:

```java
@Service
public class MyEntityService {

    private final QueryBuilder<MyEntity> queryBuilder;

    @Autowired
    public MyEntityService(EntityManager entityManager) {
        this.queryBuilder = QueryBuilder.builder(MyEntity.class, entityManager)
                .colDefs(/* Define your column definitions here */)
                .build();
    }

    @Transactional(readOnly = true)
    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) {
        // delegate AG Grid requests to query builder 
        return this.queryBuilder.getRows(request);
    }
}
```

For more examples and advanced configuration, see the [full documentation](https://smolcan.github.io/ag-grid-jpa-adapter/).


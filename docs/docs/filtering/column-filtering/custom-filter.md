---
sidebar_position: 7
---


# Custom Filter
In case of [custom filter](https://ag-grid.com/angular-data-grid/component-filter/), you need to register your own
column filter recognizer.
You can do so with [QueryBuilder](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/query/QueryBuilder.java)'s method:
```java
public QueryBuilder<E> registerCustomColumnFilterRecognizer(Function<Map<String, Object>, ColumnFilter> recognizerFunction) {
    this.customColumnFilterRecognizers.add(Objects.requireNonNull(recognizerFunction));
    return this;
}
```
The **recognizerFunction** argument is an function that receives Map object and returns:
- your custom [ColumnFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/filter/simple/ColumnFilter.java) implementation if you recognized your custom filter
- **null** otherwise

The AG Grid JPA Adapter first tries to match the **filterModel** with one of the default column filters, 
and if none of them match, tries to find the match within custom registered filters by calling them one by one and the first one that
returns other value than **null** is the match!.


**Example**
<details>
    <summary>Custom Filter example: Even and Odd custom number filter</summary>

    <details>
        <summary>Even and Odd custom filter in Angular</summary>

        ```typescript title="Angular custom filter component"
        @Component({
            selector: 'app-custom-number-filter',
            imports: [FormsModule],
            templateUrl: './custom-number-filter.component.html',
            styleUrl: './custom-number-filter.component.css'
        })
        export class CustomNumberFilterComponent implements IFilterAngularComp {
            params!: IFilterParams;
            value = 'All';

            agInit(params: IFilterParams): void {
                this.params = params;
            }

            doesFilterPass(params: IDoesFilterPassParams): boolean {
                if (this.value === 'All') {
                    return true;
                }

                const tradeId = params.data.tradeId;
                if (tradeId === null) {
                    return false;
                }

                const mod = Number(tradeId) % 2;
                if (this.value === 'Even') {
                    return mod === 0;
                } else {
                    return mod === 1;
                }
            }

            getModel(): any {
                return {
                    filterType: "customNumber",
                    value: this.value,
                }
            }

            isFilterActive(): boolean {
                return this.value === 'Even' || this.value === 'Odd';
            }

            setModel(model: any): void | AgPromise<void> {
                this.value = model?.value;
            }

            updateFilter(): void {
                this.params.filterChangedCallback();
            }
        }

        ```

        ```html title="Angular custom filter component template"
        <div class="custom-filter">
            <div>Select Year Range</div>
            <label>
                <input type="radio" name="value" [(ngModel)]="value" (ngModelChange)="updateFilter()" [value]="'All'" />
                All
            </label>
            <label>
                <input type="radio" name="value" [(ngModel)]="value" (ngModelChange)="updateFilter()" [value]="'Even'" />
                Even
            </label>
            <label>
                <input type="radio" name="value" [(ngModel)]="value" (ngModelChange)="updateFilter()" [value]="'Odd'" />
                Odd
            </label>
        </div>
        ```

        ```css title="Angular custom filter component styling"
        .custom-filter {
            width: 200px;
        }
        .custom-filter > * {
            margin: 8px;
        }
        .custom-filter > div:first-child {
            font-weight: bold;
        }
        .custom-filter > label {
            display: inline-block;
        }
        ```
    </details>


    **Create your custom filter class:**
    ```java title="Custom filter class"
    public class CustomNumberFilter extends ColumnFilter {
        
        private String value;
        
        public CustomNumberFilter() {
            super("customNumber");
        }
        
        @Override
        public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
            if (this.value == null || this.value.equalsIgnoreCase("All")) {
                return cb.and();
            }

            Path<Integer> field = root.get(columnName);
            if (this.value.equalsIgnoreCase("Even")) {
                return cb.equal(cb.mod(field, 2), 0);
            } else {
                return cb.notEqual(cb.mod(field, 2), 0);
            }
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
    ```

    **Register your column filter recognizer:**
    ```java title="Register your filter recognizer"
    QueryBuilder<YourEntityClass> queryBuilder = new QueryBuilder<>(Trade.class, entityManager)
                .registerCustomColumnFilterRecognizer((map) -> {
                    if (map.containsKey("filterType") && "customNumber".equalsIgnoreCase(map.get("filterType").toString())) {
                        CustomNumberFilter customNumberFilter = new CustomNumberFilter();
                        customNumberFilter.setValue(map.get("value").toString());
                        return customNumberFilter;
                    } else {
                        return null;
                    }
                });
    ```

</details>
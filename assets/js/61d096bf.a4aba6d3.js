"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[197],{5495:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>o,contentTitle:()=>s,default:()=>d,frontMatter:()=>a,metadata:()=>r,toc:()=>u});const r=JSON.parse('{"id":"filtering/column-filtering/custom-filter","title":"Custom Filter","description":"In case of custom filter, you need to register your own","source":"@site/docs/filtering/column-filtering/custom-filter.md","sourceDirName":"filtering/column-filtering","slug":"/filtering/column-filtering/custom-filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filtering/custom-filter","draft":false,"unlisted":false,"editUrl":"https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/docs/docs/filtering/column-filtering/custom-filter.md","tags":[],"version":"current","sidebarPosition":7,"frontMatter":{"sidebar_position":7},"sidebar":"tutorialSidebar","previous":{"title":"Multi Filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filtering/multi-filter"},"next":{"title":"Advanced Filtering","permalink":"/ag-grid-jpa-adapter/filtering/advanced-filtering/"}}');var i=t(4848),l=t(8453);const a={sidebar_position:7},s="Custom Filter",o={},u=[];function c(e){const n={a:"a",code:"code",h1:"h1",header:"header",li:"li",p:"p",pre:"pre",strong:"strong",ul:"ul",...(0,l.R)(),...e.components},{Details:t}=n;return t||function(e,n){throw new Error("Expected "+(n?"component":"object")+" `"+e+"` to be defined: you likely forgot to import, pass, or provide it.")}("Details",!0),(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.header,{children:(0,i.jsx)(n.h1,{id:"custom-filter",children:"Custom Filter"})}),"\n",(0,i.jsxs)(n.p,{children:["In case of ",(0,i.jsx)(n.a,{href:"https://ag-grid.com/angular-data-grid/component-filter/",children:"custom filter"}),", you need to register your own\ncolumn filter recognizer.\nYou can do so with ",(0,i.jsx)(n.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/query/QueryBuilder.java",children:"QueryBuilder"}),"'s method:"]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-java",children:"public QueryBuilder<E> registerCustomColumnFilterRecognizer(Function<Map<String, Object>, ColumnFilter> recognizerFunction) {\n    this.customColumnFilterRecognizers.add(Objects.requireNonNull(recognizerFunction));\n    return this;\n}\n"})}),"\n",(0,i.jsxs)(n.p,{children:["The ",(0,i.jsx)(n.strong,{children:"recognizerFunction"})," argument is an function that receives Map object and returns:"]}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsxs)(n.li,{children:["your custom ",(0,i.jsx)(n.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/filter/simple/ColumnFilter.java",children:"ColumnFilter"})," implementation if you recognized your custom filter"]}),"\n",(0,i.jsxs)(n.li,{children:[(0,i.jsx)(n.strong,{children:"null"})," otherwise"]}),"\n"]}),"\n",(0,i.jsxs)(n.p,{children:["The AG Grid JPA Adapter first tries to match the ",(0,i.jsx)(n.strong,{children:"filterModel"})," with one of the default column filters,\nand if none of them match, tries to find the match within custom registered filters by calling them one by one and the first one that\nreturns other value than ",(0,i.jsx)(n.strong,{children:"null"})," is the match!."]}),"\n",(0,i.jsx)(n.p,{children:(0,i.jsx)(n.strong,{children:"Example"})}),"\n",(0,i.jsxs)(t,{children:[(0,i.jsx)("summary",{children:"Custom Filter example: Even and Odd custom number filter"}),(0,i.jsxs)(t,{children:[(0,i.jsx)("summary",{children:"Even and Odd custom filter in Angular"}),(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-typescript",metastring:'title="Angular custom filter component"',children:"@Component({\n    selector: 'app-custom-number-filter',\n    imports: [FormsModule],\n    templateUrl: './custom-number-filter.component.html',\n    styleUrl: './custom-number-filter.component.css'\n})\nexport class CustomNumberFilterComponent implements IFilterAngularComp {\n    params!: IFilterParams;\n    value = 'All';\n\n    agInit(params: IFilterParams): void {\n        this.params = params;\n    }\n\n    doesFilterPass(params: IDoesFilterPassParams): boolean {\n        if (this.value === 'All') {\n            return true;\n        }\n\n        const tradeId = params.data.tradeId;\n        if (tradeId === null) {\n            return false;\n        }\n\n        const mod = Number(tradeId) % 2;\n        if (this.value === 'Even') {\n            return mod === 0;\n        } else {\n            return mod === 1;\n        }\n    }\n\n    getModel(): any {\n        return {\n            filterType: \"customNumber\",\n            value: this.value,\n        }\n    }\n\n    isFilterActive(): boolean {\n        return this.value === 'Even' || this.value === 'Odd';\n    }\n\n    setModel(model: any): void | AgPromise<void> {\n        this.value = model?.value;\n    }\n\n    updateFilter(): void {\n        this.params.filterChangedCallback();\n    }\n}\n\n"})}),(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-html",metastring:'title="Angular custom filter component template"',children:'<div class="custom-filter">\n    <div>Select Year Range</div>\n    <label>\n        <input type="radio" name="value" [(ngModel)]="value" (ngModelChange)="updateFilter()" [value]="\'All\'" />\n        All\n    </label>\n    <label>\n        <input type="radio" name="value" [(ngModel)]="value" (ngModelChange)="updateFilter()" [value]="\'Even\'" />\n        Even\n    </label>\n    <label>\n        <input type="radio" name="value" [(ngModel)]="value" (ngModelChange)="updateFilter()" [value]="\'Odd\'" />\n        Odd\n    </label>\n</div>\n'})}),(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-css",metastring:'title="Angular custom filter component styling"',children:".custom-filter {\n    width: 200px;\n}\n.custom-filter > * {\n    margin: 8px;\n}\n.custom-filter > div:first-child {\n    font-weight: bold;\n}\n.custom-filter > label {\n    display: inline-block;\n}\n"})})]}),(0,i.jsx)(n.p,{children:(0,i.jsx)(n.strong,{children:"Create your custom filter class:"})}),(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-java",metastring:'title="Custom filter class"',children:'public class CustomNumberFilter extends ColumnFilter {\n    \n    private String value;\n    \n    public CustomNumberFilter() {\n        super("customNumber");\n    }\n    \n    @Override\n    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {\n        if (this.value == null || this.value.equalsIgnoreCase("All")) {\n            return cb.and();\n        }\n\n        Path<Integer> field = root.get(columnName);\n        if (this.value.equalsIgnoreCase("Even")) {\n            return cb.equal(cb.mod(field, 2), 0);\n        } else {\n            return cb.notEqual(cb.mod(field, 2), 0);\n        }\n    }\n\n    public String getValue() {\n        return value;\n    }\n\n    public void setValue(String value) {\n        this.value = value;\n    }\n}\n'})}),(0,i.jsx)(n.p,{children:(0,i.jsx)(n.strong,{children:"Register your column filter recognizer:"})}),(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-java",metastring:'title="Register your filter recognizer"',children:'QueryBuilder<YourEntityClass> queryBuilder = new QueryBuilder<>(Trade.class, entityManager)\n            .registerCustomColumnFilterRecognizer((map) -> {\n                if (map.containsKey("filterType") && "customNumber".equalsIgnoreCase(map.get("filterType").toString())) {\n                    CustomNumberFilter customNumberFilter = new CustomNumberFilter();\n                    customNumberFilter.setValue(map.get("value").toString());\n                    return customNumberFilter;\n                } else {\n                    return null;\n                }\n            });\n'})})]})]})}function d(e={}){const{wrapper:n}={...(0,l.R)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(c,{...e})}):c(e)}},8453:(e,n,t)=>{t.d(n,{R:()=>a,x:()=>s});var r=t(6540);const i={},l=r.createContext(i);function a(e){const n=r.useContext(l);return r.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function s(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:a(e.components),r.createElement(l.Provider,{value:n},e.children)}}}]);
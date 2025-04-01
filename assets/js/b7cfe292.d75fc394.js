"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[97],{9956:(e,r,t)=>{t.r(r),t.d(r,{assets:()=>o,contentTitle:()=>a,default:()=>m,frontMatter:()=>s,metadata:()=>n,toc:()=>c});const n=JSON.parse('{"id":"filtering/column-filter/custom-filter","title":"Custom Filter","description":"To implement a custom filter, follow these steps:","source":"@site/docs/filtering/column-filter/custom-filter.md","sourceDirName":"filtering/column-filter","slug":"/filtering/column-filter/custom-filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filter/custom-filter","draft":false,"unlisted":false,"editUrl":"https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/docs/docs/filtering/column-filter/custom-filter.md","tags":[],"version":"current","sidebarPosition":6,"frontMatter":{"sidebar_position":6},"sidebar":"tutorialSidebar","previous":{"title":"Multi Filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filter/multi-filter"},"next":{"title":"Advanced Filter","permalink":"/ag-grid-jpa-adapter/filtering/advanced-filter/"}}');var l=t(4848),i=t(8453);const s={sidebar_position:6},a="Custom Filter",o={},c=[{value:"Example",id:"example",level:2},{value:"1. <strong>Implement the <code>IFilterModel</code> interface</strong>",id:"1-implement-the-ifiltermodel-interface",level:3},{value:"2.  <strong>Implement the <code>IFilterParams</code> interface</strong>",id:"2--implement-the-ifilterparams-interface",level:3},{value:"3. <strong>Extend the <code>IFilter</code> abstract class</strong>",id:"3-extend-the-ifilter-abstract-class",level:3},{value:"4. Using your custom filter in ColDefs",id:"4-using-your-custom-filter-in-coldefs",level:3}];function d(e){const r={a:"a",br:"br",code:"code",h1:"h1",h2:"h2",h3:"h3",header:"header",li:"li",ol:"ol",p:"p",pre:"pre",strong:"strong",...(0,i.R)(),...e.components};return(0,l.jsxs)(l.Fragment,{children:[(0,l.jsx)(r.header,{children:(0,l.jsx)(r.h1,{id:"custom-filter",children:"Custom Filter"})}),"\n",(0,l.jsx)(r.p,{children:"To implement a custom filter, follow these steps:"}),"\n",(0,l.jsxs)(r.ol,{children:["\n",(0,l.jsxs)(r.li,{children:["\n",(0,l.jsxs)(r.p,{children:[(0,l.jsxs)(r.strong,{children:["Implement the ",(0,l.jsx)(r.code,{children:"IFilterModel"})," interface"]}),(0,l.jsx)(r.br,{}),"\n","Define your custom filter model by creating a class that implements the ",(0,l.jsx)(r.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/IFilterModel.java",children:(0,l.jsx)(r.code,{children:"IFilterModel"})})," interface."]}),"\n"]}),"\n",(0,l.jsxs)(r.li,{children:["\n",(0,l.jsxs)(r.p,{children:[(0,l.jsxs)(r.strong,{children:["Implement the ",(0,l.jsx)(r.code,{children:"IFilterParams"})," interface"]}),(0,l.jsx)(r.br,{}),"\n","Create a class representing the parameters for your filter by implementing the ",(0,l.jsx)(r.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/IFilterParams.java",children:(0,l.jsx)(r.code,{children:"IFilterParams"})})," interface."]}),"\n"]}),"\n",(0,l.jsxs)(r.li,{children:["\n",(0,l.jsxs)(r.p,{children:[(0,l.jsxs)(r.strong,{children:["Extend the ",(0,l.jsx)(r.code,{children:"IFilter"})," abstract class"]}),(0,l.jsx)(r.br,{}),"\n","Create a class representing your custom filter by extending the ",(0,l.jsx)(r.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/IFilter.java",children:(0,l.jsx)(r.code,{children:"IFilter"})})," abstract class."]}),"\n"]}),"\n"]}),"\n",(0,l.jsx)(r.h2,{id:"example",children:"Example"}),"\n",(0,l.jsx)(r.p,{children:"We will create a custom number filter, that will filter the even/odd numbers."}),"\n",(0,l.jsxs)(r.h3,{id:"1-implement-the-ifiltermodel-interface",children:["1. ",(0,l.jsxs)(r.strong,{children:["Implement the ",(0,l.jsx)(r.code,{children:"IFilterModel"})," interface"]})]}),"\n",(0,l.jsx)(r.p,{children:"Our filter model will have only one field, which is value (Even, Odd or All)."}),"\n",(0,l.jsx)(r.pre,{children:(0,l.jsx)(r.code,{className:"language-java",children:"public class CustomNumberFilterModel implements IFilterModel {\n    private String value;\n    // ...getters and setters\n}\n"})}),"\n",(0,l.jsxs)(r.h3,{id:"2--implement-the-ifilterparams-interface",children:["2.  ",(0,l.jsxs)(r.strong,{children:["Implement the ",(0,l.jsx)(r.code,{children:"IFilterParams"})," interface"]})]}),"\n",(0,l.jsx)(r.p,{children:"Additional parameters for our filter, for example, if we should include null values in results."}),"\n",(0,l.jsx)(r.pre,{children:(0,l.jsx)(r.code,{className:"language-java",children:"public class CustomNumberFilterParams implements IFilterParams {\n    private boolean includeNullValues;\n    // ...getters and setters\n}\n"})}),"\n",(0,l.jsxs)(r.h3,{id:"3-extend-the-ifilter-abstract-class",children:["3. ",(0,l.jsxs)(r.strong,{children:["Extend the ",(0,l.jsx)(r.code,{children:"IFilter"})," abstract class"]})]}),"\n",(0,l.jsx)(r.p,{children:"Extend and overwrite the required methods."}),"\n",(0,l.jsx)(r.pre,{children:(0,l.jsx)(r.code,{className:"language-java",children:'public class CustomNumberFilter extends IFilter<CustomNumberFilterModel, CustomNumberFilterParams> {\n    \n    @Override\n    // map to filter model object\n    public CustomNumberFilterModel recognizeFilterModel(Map<String, Object> map) {\n        CustomNumberFilterModel customNumberFilter = new CustomNumberFilterModel();\n        customNumberFilter.setValue(map.get("value").toString());\n        return customNumberFilter;\n    }\n\n    @Override\n    // default params with default values\n    public CustomNumberFilterParams getDefaultFilterParams() {\n        return new CustomNumberFilterParams();\n    }\n\n    @Override\n    // create predicate from expression\n    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, CustomNumberFilterModel customNumberFilterModel) {\n        String value = customNumberFilterModel.getValue();\n        if (value == null || value.equalsIgnoreCase("All")) {\n            // not active, always true\n            return cb.and();\n        }\n\n        Expression<Integer> integerExpression = expression.as(Integer.class);\n        Predicate predicate;\n        if (value.equalsIgnoreCase("Even")) {\n            predicate = cb.equal(cb.mod(integerExpression, 2), 0);\n        } else {\n            predicate = cb.notEqual(cb.mod(integerExpression, 2), 0);\n        }\n        \n        // predicate also depends on filter params\n        if (this.filterParams.isIncludeNullValues()) {\n            predicate = cb.or(predicate, cb.isNull(expression));\n        }\n        \n        return predicate;\n    }\n}\n'})}),"\n",(0,l.jsx)(r.h3,{id:"4-using-your-custom-filter-in-coldefs",children:"4. Using your custom filter in ColDefs"}),"\n",(0,l.jsx)(r.p,{children:"Set your filter with filter params to column definition."}),"\n",(0,l.jsx)(r.pre,{children:(0,l.jsx)(r.code,{className:"language-java",children:'boolean includeNullValues = true;\n\nColDef colDef = ColDef.builder()\n    .field("tradeId")\n    .filter(\n        new CustomNumberFilter()\n            .filterParams(new CustomNumberFilterParams(includeNullValues))\n    )\n    .build();\n'})})]})}function m(e={}){const{wrapper:r}={...(0,i.R)(),...e.components};return r?(0,l.jsx)(r,{...e,children:(0,l.jsx)(d,{...e})}):d(e)}},8453:(e,r,t)=>{t.d(r,{R:()=>s,x:()=>a});var n=t(6540);const l={},i=n.createContext(l);function s(e){const r=n.useContext(i);return n.useMemo((function(){return"function"==typeof e?e(r):{...r,...e}}),[r,e])}function a(e){let r;return r=e.disableParentContext?"function"==typeof e.components?e.components(l):e.components||l:s(e.components),n.createElement(i.Provider,{value:r},e.children)}}}]);
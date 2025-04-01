"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[169],{4640:(e,a,r)=>{r.r(a),r.d(a,{assets:()=>s,contentTitle:()=>d,default:()=>p,frontMatter:()=>l,metadata:()=>t,toc:()=>c});const t=JSON.parse('{"id":"filtering/advanced-filter/advanced-filter","title":"Advanced Filter","description":"Enable Advanced filter","source":"@site/docs/filtering/advanced-filter/advanced-filter.md","sourceDirName":"filtering/advanced-filter","slug":"/filtering/advanced-filter/","permalink":"/ag-grid-jpa-adapter/filtering/advanced-filter/","draft":false,"unlisted":false,"editUrl":"https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/docs/docs/filtering/advanced-filter/advanced-filter.md","tags":[],"version":"current","sidebarPosition":5,"frontMatter":{"sidebar_position":5},"sidebar":"tutorialSidebar","previous":{"title":"Custom Filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filter/custom-filter"},"next":{"title":"Row Grouping","permalink":"/ag-grid-jpa-adapter/row-grouping"}}');var i=r(4848),n=r(8453);const l={sidebar_position:5},d="Advanced Filter",s={},c=[{value:"Enable Advanced filter",id:"enable-advanced-filter",level:2},{value:"Filter Params",id:"filter-params",level:2}];function o(e){const a={a:"a",code:"code",h1:"h1",h2:"h2",header:"header",li:"li",p:"p",pre:"pre",strong:"strong",ul:"ul",...(0,n.R)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(a.header,{children:(0,i.jsx)(a.h1,{id:"advanced-filter",children:"Advanced Filter"})}),"\n",(0,i.jsx)(a.h2,{id:"enable-advanced-filter",children:"Enable Advanced filter"}),"\n",(0,i.jsxs)(a.p,{children:["To enable the advanced filter, set the value of the ",(0,i.jsx)(a.code,{children:"enableAdvancedFilter"})," variable in ",(0,i.jsx)(a.code,{children:"QueryBuilder"})," to ",(0,i.jsx)(a.code,{children:"true"}),":"]}),"\n",(0,i.jsx)(a.pre,{children:(0,i.jsx)(a.code,{className:"language-java",children:"this.queryBuilder = QueryBuilder.builder(Entity.class, entityManager)\n                .colDefs(\n                        // colDefs\n                )\n                .enableAdvancedFilter(true) // enable advanced filtering\n                .build();\n"})}),"\n",(0,i.jsxs)(a.p,{children:["For a column to be filterable in the Advanced Filter, it must have a filter defined in its ",(0,i.jsx)(a.code,{children:"ColDef"}),"."]}),"\n",(0,i.jsxs)(a.p,{children:["If a column does not have a filter set in ",(0,i.jsx)(a.code,{children:"ColDef"}),", attempting to apply an Advanced Filter on it will result in an exception."]}),"\n",(0,i.jsx)(a.h2,{id:"filter-params",children:"Filter Params"}),"\n",(0,i.jsxs)(a.p,{children:[(0,i.jsx)(a.strong,{children:"Filter parameters"})," are taken from ",(0,i.jsx)(a.code,{children:"ColDef"}),"."]}),"\n",(0,i.jsxs)(a.ul,{children:["\n",(0,i.jsxs)(a.li,{children:[(0,i.jsx)(a.strong,{children:"Text & Object Filters"})," \u2192 ",(0,i.jsx)(a.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/TextFilterParams.java",children:"TextFilterParams"})]}),"\n",(0,i.jsxs)(a.li,{children:[(0,i.jsx)(a.strong,{children:"Date & DateString Filters"})," \u2192 ",(0,i.jsx)(a.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/DateFilterParams.java",children:"DateFilterParams"})]}),"\n",(0,i.jsxs)(a.li,{children:[(0,i.jsx)(a.strong,{children:"Number Filters"})," \u2192 ",(0,i.jsx)(a.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/NumberFilterParams.java",children:"NumberFilterParams"})]}),"\n",(0,i.jsxs)(a.li,{children:[(0,i.jsx)(a.strong,{children:"Boolean Filters"})," \u2192 No filter parameters"]}),"\n"]})]})}function p(e={}){const{wrapper:a}={...(0,n.R)(),...e.components};return a?(0,i.jsx)(a,{...e,children:(0,i.jsx)(o,{...e})}):o(e)}},8453:(e,a,r)=>{r.d(a,{R:()=>l,x:()=>d});var t=r(6540);const i={},n=t.createContext(i);function l(e){const a=t.useContext(n);return t.useMemo((function(){return"function"==typeof e?e(a):{...a,...e}}),[a,e])}function d(e){let a;return a=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:l(e.components),t.createElement(n.Provider,{value:a},e.children)}}}]);
"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[43],{3776:(e,i,t)=>{t.r(i),t.d(i,{assets:()=>s,contentTitle:()=>o,default:()=>f,frontMatter:()=>a,metadata:()=>n,toc:()=>d});const n=JSON.parse('{"id":"filtering/column-filtering/column-filtering","title":"Column Filtering","description":"If received filter model is in this format:","source":"@site/docs/filtering/column-filtering/column-filtering.md","sourceDirName":"filtering/column-filtering","slug":"/filtering/column-filtering/","permalink":"/ag-grid-jpa-adapter/filtering/column-filtering/","draft":false,"unlisted":false,"editUrl":"https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/docs/docs/filtering/column-filtering/column-filtering.md","tags":[],"version":"current","sidebarPosition":1,"frontMatter":{"sidebar_position":1},"sidebar":"tutorialSidebar","previous":{"title":"Filtering","permalink":"/ag-grid-jpa-adapter/filtering/"},"next":{"title":"Text Filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filtering/text-filter"}}');var r=t(4848),l=t(8453);const a={sidebar_position:1},o="Column Filtering",s={},d=[];function c(e){const i={a:"a",code:"code",h1:"h1",header:"header",li:"li",p:"p",pre:"pre",strong:"strong",ul:"ul",...(0,l.R)(),...e.components};return(0,r.jsxs)(r.Fragment,{children:[(0,r.jsx)(i.header,{children:(0,r.jsx)(i.h1,{id:"column-filtering",children:"Column Filtering"})}),"\n",(0,r.jsx)(i.p,{children:"If received filter model is in this format:"}),"\n",(0,r.jsx)(i.pre,{children:(0,r.jsx)(i.code,{className:"language-typescript",metastring:'title="Column Filter format"',children:"export interface FilterModel {\n    [colId: string]: any;\n}\n"})}),"\n",(0,r.jsx)(i.p,{children:"then we know we received column filter.\nEvery key of the object is the column id, and every value is the filter model."}),"\n",(0,r.jsx)(i.p,{children:"For example:"}),"\n",(0,r.jsx)(i.pre,{children:(0,r.jsx)(i.code,{className:"language-javascript",metastring:'title="Example of column filter model from AG Grid documentation"',children:"{\n    filterModel: {\n        athlete: {\n            filterType: 'text',\n            type: 'contains',\n            filter: 'fred'\n        },\n        year: {\n            filterType: 'number',\n            type: 'greaterThan',\n            filter: 2005,\n            filterTo: null\n        }\n    }\n}\n"})}),"\n",(0,r.jsxs)(i.p,{children:["All of the column filters contains ",(0,r.jsx)(i.strong,{children:"filterType"})," field.\nDefault built-in filters in AG Grid are:"]}),"\n",(0,r.jsxs)(i.ul,{children:["\n",(0,r.jsx)(i.li,{children:(0,r.jsx)(i.a,{href:"https://ag-grid.com/angular-data-grid/filter-text/",children:"Text Filter"})}),"\n",(0,r.jsx)(i.li,{children:(0,r.jsx)(i.a,{href:"https://ag-grid.com/angular-data-grid/filter-number/",children:"Number Filter"})}),"\n",(0,r.jsx)(i.li,{children:(0,r.jsx)(i.a,{href:"https://ag-grid.com/angular-data-grid/filter-date/",children:"Date Filter"})}),"\n",(0,r.jsx)(i.li,{children:"Combined Filter"}),"\n",(0,r.jsx)(i.li,{children:(0,r.jsx)(i.a,{href:"https://ag-grid.com/angular-data-grid/filter-set/",children:"Set Filter"})}),"\n",(0,r.jsx)(i.li,{children:(0,r.jsx)(i.a,{href:"https://ag-grid.com/angular-data-grid/filter-multi/",children:"Multi Filter"})}),"\n"]}),"\n",(0,r.jsx)(i.p,{children:"These filters are recognized and applied in this solution out of the box without any further configuration needed."}),"\n",(0,r.jsxs)(i.p,{children:["In AG Grid JPA Adapter is column filter implemented in class ",(0,r.jsx)(i.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/filter/simple/ColumnFilter.java",children:"ColumnFilter"}),"."]})]})}function f(e={}){const{wrapper:i}={...(0,l.R)(),...e.components};return i?(0,r.jsx)(i,{...e,children:(0,r.jsx)(c,{...e})}):c(e)}},8453:(e,i,t)=>{t.d(i,{R:()=>a,x:()=>o});var n=t(6540);const r={},l=n.createContext(r);function a(e){const i=n.useContext(l);return n.useMemo((function(){return"function"==typeof e?e(i):{...i,...e}}),[i,e])}function o(e){let i;return i=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:a(e.components),n.createElement(l.Provider,{value:i},e.children)}}}]);
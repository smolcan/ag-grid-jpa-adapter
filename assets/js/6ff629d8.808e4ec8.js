"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[896],{5073:(e,r,t)=>{t.r(r),t.d(r,{assets:()=>d,contentTitle:()=>s,default:()=>g,frontMatter:()=>a,metadata:()=>n,toc:()=>c});const n=JSON.parse('{"id":"sorting","title":"Sorting","description":"Sorting is performed according to received sortModel in ServerSideGetRowsRequest","source":"@site/docs/sorting.md","sourceDirName":".","slug":"/sorting","permalink":"/ag-grid-jpa-adapter/sorting","draft":false,"unlisted":false,"editUrl":"https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/docs/docs/sorting.md","tags":[],"version":"current","sidebarPosition":2,"frontMatter":{"sidebar_position":2},"sidebar":"tutorialSidebar","previous":{"title":"Quick Start","permalink":"/ag-grid-jpa-adapter/"},"next":{"title":"Filtering","permalink":"/ag-grid-jpa-adapter/filtering"}}');var i=t(4848),o=t(8453);const a={sidebar_position:2},s="Sorting",d={},c=[];function l(e){const r={a:"a",code:"code",h1:"h1",header:"header",p:"p",pre:"pre",strong:"strong",...(0,o.R)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(r.header,{children:(0,i.jsx)(r.h1,{id:"sorting",children:"Sorting"})}),"\n",(0,i.jsxs)(r.p,{children:[(0,i.jsx)(r.a,{href:"https://ag-grid.com/angular-data-grid/server-side-model-sorting/",children:"Sorting"})," is performed according to received ",(0,i.jsx)(r.strong,{children:"sortModel"})," in ",(0,i.jsx)(r.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java",children:"ServerSideGetRowsRequest"})]}),"\n",(0,i.jsx)(r.pre,{children:(0,i.jsx)(r.code,{className:"language-javascript",metastring:'title="Sort model example from AG Grid documentation"',children:"{\n    sortModel: [\n        { colId: 'country', sort: 'asc' },\n        { colId: 'year', sort: 'desc' },\n    ]\n}\n"})}),"\n",(0,i.jsxs)(r.p,{children:["Sorting is implemented in ",(0,i.jsx)(r.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/query/QueryBuilder.java",children:"QueryBuilder"}),"'s ",(0,i.jsx)(r.strong,{children:"orderBy"})," method,\nwhich copies the default sorting behaviour from the ",(0,i.jsx)(r.a,{href:"https://ag-grid.com/angular-data-grid/server-side-model-sorting/",children:"official AG Grid documentation"}),".\nIf different behaviour is needed, this method can be overwritten."]})]})}function g(e={}){const{wrapper:r}={...(0,o.R)(),...e.components};return r?(0,i.jsx)(r,{...e,children:(0,i.jsx)(l,{...e})}):l(e)}},8453:(e,r,t)=>{t.d(r,{R:()=>a,x:()=>s});var n=t(6540);const i={},o=n.createContext(i);function a(e){const r=n.useContext(o);return n.useMemo((function(){return"function"==typeof e?e(r):{...r,...e}}),[r,e])}function s(e){let r;return r=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:a(e.components),n.createElement(o.Provider,{value:r},e.children)}}}]);
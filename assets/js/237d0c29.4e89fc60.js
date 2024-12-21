"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[111],{7583:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>d,contentTitle:()=>a,default:()=>p,frontMatter:()=>o,metadata:()=>i,toc:()=>s});const i=JSON.parse('{"id":"filtering/column-filtering/combined-filter","title":"Combined Filter","description":"If more than one Filter Condition is set, then multiple instances of the model are created and wrapped inside a Combined Model.","source":"@site/docs/filtering/column-filtering/combined-filter.md","sourceDirName":"filtering/column-filtering","slug":"/filtering/column-filtering/combined-filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filtering/combined-filter","draft":false,"unlisted":false,"editUrl":"https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/docs/docs/filtering/column-filtering/combined-filter.md","tags":[],"version":"current","sidebarPosition":4,"frontMatter":{"sidebar_position":4},"sidebar":"tutorialSidebar","previous":{"title":"Date Filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filtering/date-filter"},"next":{"title":"Set Filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filtering/set-filter"}}');var r=t(4848),l=t(8453);const o={sidebar_position:4},a="Combined Filter",d={},s=[];function c(e){const n={a:"a",code:"code",h1:"h1",header:"header",p:"p",pre:"pre",strong:"strong",...(0,l.R)(),...e.components};return(0,r.jsxs)(r.Fragment,{children:[(0,r.jsx)(n.header,{children:(0,r.jsx)(n.h1,{id:"combined-filter",children:"Combined Filter"})}),"\n",(0,r.jsx)(n.p,{children:"If more than one Filter Condition is set, then multiple instances of the model are created and wrapped inside a Combined Model."}),"\n",(0,r.jsx)(n.pre,{children:(0,r.jsx)(n.code,{className:"language-typescript",metastring:'title="Combined Filter model interface definition"',children:"export interface ICombinedSimpleModel<M extends ISimpleFilterModel> extends ProvidedFilterModel {\n    operator: JoinOperator;\n    conditions: M[];\n}\nexport type JoinOperator = 'AND' | 'OR';\n"})}),"\n",(0,r.jsxs)(n.p,{children:["As you can see from the interface definition, it can wrap multiple instances of filters extending ",(0,r.jsx)(n.strong,{children:"ISimpleFilterModel"})," and join them by ",(0,r.jsx)(n.strong,{children:"JoinOperator"}),".\n",(0,r.jsx)(n.strong,{children:"ISimpleFilterModel"})," is extended only by ",(0,r.jsx)(n.a,{href:"/ag-grid-jpa-adapter/filtering/column-filtering/text-filter",children:"TextFilter"}),", ",(0,r.jsx)(n.a,{href:"/ag-grid-jpa-adapter/filtering/column-filtering/number-filter",children:"NumberFilter"})," and ",(0,r.jsx)(n.a,{href:"/ag-grid-jpa-adapter/filtering/column-filtering/date-filter",children:"DateFilter"})," models."]}),"\n",(0,r.jsx)(n.pre,{children:(0,r.jsx)(n.code,{className:"language-javascript",metastring:'title="Combined Filter model example for Text, Number and Date filter"',children:"{\n    filterModel: {\n        // Combined Text Filter\n        sport: {\n            filterType: 'text',\n            operator: 'OR',\n            conditions: [\n                {\n                    filterType: 'text',\n                    type: 'equals',\n                    filter: 'Swimming'\n                },\n                {\n                    filterType: 'text',\n                    type: 'equals',\n                    filter: 'Gymnastics'\n                }\n            ]\n        },\n        // Combined Number Filter\n        timeInSeconds: {\n            filterType: 'number',\n            operator: 'OR',\n            conditions: [\n                {\n                    filterType: 'number',\n                    type: 'equals',\n                    filter: 18\n                },\n                {\n                    filterType: 'number',\n                    type: 'equals',\n                    filter: 20\n                }\n            ]\n        },\n        // Combined Date Filter\n        eventDate: {\n            filterType: 'date',\n            operator: 'OR',\n            conditions: [\n                {\n                    filterType: 'date',\n                    type: 'equals',\n                    dateFrom: '2004-08-29'\n                },\n                {\n                    filterType: 'date',\n                    type: 'equals',\n                    dateFrom: '2008-08-24'\n                }\n            ]\n        }\n    }\n}\n"})}),"\n",(0,r.jsxs)(n.p,{children:["Combined filter is recognized as it always has ",(0,r.jsx)(n.strong,{children:"filterType"})," property that has value  ",(0,r.jsx)(n.strong,{children:"'text'"}),", ",(0,r.jsx)(n.strong,{children:"'number'"})," or ",(0,r.jsx)(n.strong,{children:"'date'"})," and\nalso has ",(0,r.jsx)(n.strong,{children:"operator"})," field and ",(0,r.jsx)(n.strong,{children:"conditions"})," field."]}),"\n",(0,r.jsxs)(n.p,{children:["In AG Grid JPA Adapter is date filter implemented in class ",(0,r.jsx)(n.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/filter/simple/CombinedSimpleModel.java",children:"CombinedSimpleModel"}),"."]})]})}function p(e={}){const{wrapper:n}={...(0,l.R)(),...e.components};return n?(0,r.jsx)(n,{...e,children:(0,r.jsx)(c,{...e})}):c(e)}},8453:(e,n,t)=>{t.d(n,{R:()=>o,x:()=>a});var i=t(6540);const r={},l=i.createContext(r);function o(e){const n=i.useContext(l);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function a(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(r):e.components||r:o(e.components),i.createElement(l.Provider,{value:n},e.children)}}}]);
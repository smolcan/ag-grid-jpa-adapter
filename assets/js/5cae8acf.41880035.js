"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[483],{477:(e,n,r)=>{r.r(n),r.d(n,{assets:()=>d,contentTitle:()=>a,default:()=>p,frontMatter:()=>s,metadata:()=>t,toc:()=>l});const t=JSON.parse('{"id":"grouping","title":"Grouping","description":"We receive information about Row grouping in ServerSideGetRowsRequest in these two fields:","source":"@site/docs/grouping.md","sourceDirName":".","slug":"/grouping","permalink":"/ag-grid-jpa-adapter/grouping","draft":false,"unlisted":false,"editUrl":"https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/docs/docs/grouping.md","tags":[],"version":"current","sidebarPosition":3,"frontMatter":{"sidebar_position":3},"sidebar":"tutorialSidebar","previous":{"title":"Date Advanced Filter Model","permalink":"/ag-grid-jpa-adapter/filtering/advanced-filtering/column-advanced-filter-model/date-advanced-filter-model"},"next":{"title":"Pivoting","permalink":"/ag-grid-jpa-adapter/pivoting"}}');var i=r(4848),o=r(8453);const s={sidebar_position:3},a="Grouping",d={},l=[{value:"Grouping Example",id:"grouping-example",level:2},{value:"Aggregated fields",id:"aggregated-fields",level:2}];function c(e){const n={a:"a",code:"code",h1:"h1",h2:"h2",header:"header",img:"img",li:"li",p:"p",pre:"pre",strong:"strong",ul:"ul",...(0,o.R)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.header,{children:(0,i.jsx)(n.h1,{id:"grouping",children:"Grouping"})}),"\n",(0,i.jsxs)(n.p,{children:["We receive information about ",(0,i.jsx)(n.a,{href:"https://ag-grid.com/react-data-grid/server-side-model-grouping/",children:"Row grouping"})," in ",(0,i.jsx)(n.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java",children:"ServerSideGetRowsRequest"})," in these two fields:"]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-java",metastring:'title="Grouping information in ServerSideGetRowsRequest"',children:"public class ServerSideGetRowsRequest {\n    // ... other params\n\n    // Columns that are currently row grouped. \n    private List<ColumnVO> rowGroupCols = Collections.emptyList();\n    // What groups the user is viewing.\n    private List<String> groupKeys = Collections.emptyList();\n    \n    // ... other params\n}\n"})}),"\n",(0,i.jsxs)(n.p,{children:[(0,i.jsx)(n.strong,{children:"rowGroupCols"})," tells us which columns are grouped and\n",(0,i.jsx)(n.strong,{children:"groupKeys"})," tells us about expanded groups and their keys."]}),"\n",(0,i.jsxs)(n.p,{children:["In ",(0,i.jsx)(n.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/query/QueryBuilder.java",children:"QueryBuilder"}),", this functionality\nis implemented in method ",(0,i.jsx)(n.strong,{children:"groupBy"})," for grouping data and in method ",(0,i.jsx)(n.strong,{children:"where"})," to filter data of expanded group."]}),"\n",(0,i.jsx)(n.h2,{id:"grouping-example",children:"Grouping Example"}),"\n",(0,i.jsxs)(n.p,{children:[(0,i.jsx)(n.strong,{children:"Grid with grouping and all groups are collapsed:"}),"\n",(0,i.jsx)(n.img,{alt:"Grid with grouping and all groups are collapsed",src:r(1895).A+"",width:"796",height:"268"})]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-JSON",metastring:'title="Payload when all groups are collapsed"',children:'{\n  "rowGroupCols": [\n    {\n      "id": "product",\n      "displayName": "Product",\n      "field": "product"\n    },\n    {\n      "id": "portfolio",\n      "displayName": "Portfolio",\n      "field": "portfolio"\n    }\n  ],\n  "groupKeys": []\n}\n'})}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-SQL",metastring:'title="Generated SQL select with all collapsed groups" ',children:"select\n    t.product \nfrom\n    trade t\ngroup by\n    t.product \noffset\n    0 rows \nfetch\n    first 101 rows only\n"})}),"\n",(0,i.jsxs)(n.p,{children:[(0,i.jsx)(n.strong,{children:"Expanded first group:"}),"\n",(0,i.jsx)(n.img,{alt:"Grid with grouping and expanded first group",src:r(3164).A+"",width:"796",height:"268"})]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-JSON",metastring:'title="Payload when expanded first group with key ProductA"',children:'{\n  "rowGroupCols": [\n    {\n      "id": "product",\n      "displayName": "Product",\n      "field": "product"\n    },\n    {\n      "id": "portfolio",\n      "displayName": "Portfolio",\n      "field": "portfolio"\n    }\n  ],\n  "groupKeys": [\n    "ProductA"\n  ]\n}\n'})}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-SQL",metastring:'title="Generated SQL select with one expanded group" ',children:"select\n    t.product,\n    t.portfolio \nfrom\n    trade t \nwhere\n    t.product='ProductA' \ngroup by\n    t.product,\n    t.portfolio \noffset\n    0 rows \nfetch\n    first 101 rows only\n"})}),"\n",(0,i.jsxs)(n.p,{children:[(0,i.jsx)(n.strong,{children:"Expanded second group:"}),"\n",(0,i.jsx)(n.img,{alt:"Grid with grouping and expanded second group",src:r(677).A+"",width:"796",height:"267"})]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-JSON",metastring:'title="Payload when expanded also second group with key Portfolio1"',children:'{\n  "rowGroupCols": [\n    {\n      "id": "product",\n      "displayName": "Product",\n      "field": "product"\n    },\n    {\n      "id": "portfolio",\n      "displayName": "Portfolio",\n      "field": "portfolio"\n    }\n  ],\n  "groupKeys": [\n    "ProductA",\n    "Portfolio1"\n  ]\n}\n'})}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-SQL",metastring:'title="Generated SQL select with all expanded groups (number of grouped cols is same as number of group keys)" ',children:"select\n    t.pl1,\n    t.birth_date,\n    t.previous_value,\n    t.deal_type,\n    t.submitter_deal_id,\n    t.product,\n    t.current_value,\n    t.gain_dx,\n    t.pl2,\n    t.submitter_id,\n    t.bid_type,\n    t.is_sold,\n    t.trade_id,\n    t.x99_out,\n    t.book,\n    t.portfolio,\n    t.sx_px,\n    t.batch \nfrom\n    trade t \nwhere\n    t.product='ProductA'\n    and t.portfolio='Portfolio1'\noffset\n    0 rows \nfetch\n    first 101 rows only\n"})}),"\n",(0,i.jsx)(n.h2,{id:"aggregated-fields",children:"Aggregated fields"}),"\n",(0,i.jsxs)(n.p,{children:["We receive aggregated fields in ",(0,i.jsx)(n.strong,{children:"valueCols"})," field."]}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{className:"language-java",metastring:'title="Aggregation information in ServerSideGetRowsRequest"',children:"public class ServerSideGetRowsRequest {\n    // ... other params\n\n    // Columns that have aggregations on them.\n    private List<ColumnVO> valueCols = Collections.emptyList();    \n    \n    // ... other params\n}\n"})}),"\n",(0,i.jsx)(n.p,{children:"Possible aggregated functions:"}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:"avg"}),"\n",(0,i.jsx)(n.li,{children:"sum"}),"\n",(0,i.jsx)(n.li,{children:"min"}),"\n",(0,i.jsx)(n.li,{children:"max"}),"\n",(0,i.jsx)(n.li,{children:"count"}),"\n",(0,i.jsx)(n.li,{children:"first (not supported by JPA)"}),"\n",(0,i.jsx)(n.li,{children:"last (not supported by JPA)"}),"\n"]})]})}function p(e={}){const{wrapper:n}={...(0,o.R)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(c,{...e})}):c(e)}},1895:(e,n,r)=>{r.d(n,{A:()=>t});const t=r.p+"assets/images/group_img_1-552fb1ecd96ccd68e4f7437cb1f5ce50.png"},3164:(e,n,r)=>{r.d(n,{A:()=>t});const t=r.p+"assets/images/group_img_2-08264ac61c1aa242fe0fa287a57a804f.png"},677:(e,n,r)=>{r.d(n,{A:()=>t});const t=r.p+"assets/images/group_img_3-99fe9c055cabee4fc8b524c5c97cf510.png"},8453:(e,n,r)=>{r.d(n,{R:()=>s,x:()=>a});var t=r(6540);const i={},o=t.createContext(i);function s(e){const n=t.useContext(o);return t.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function a(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:s(e.components),t.createElement(o.Provider,{value:n},e.children)}}}]);
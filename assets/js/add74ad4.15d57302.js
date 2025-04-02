"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[502],{8541:(e,t,r)=>{r.r(t),r.d(t,{assets:()=>o,contentTitle:()=>a,default:()=>h,frontMatter:()=>s,metadata:()=>i,toc:()=>d});const i=JSON.parse('{"id":"filtering/column-filter/text-filter","title":"Text Filter","description":"Text Filters allow you to filter string data.","source":"@site/docs/filtering/column-filter/text-filter.md","sourceDirName":"filtering/column-filter","slug":"/filtering/column-filter/text-filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filter/text-filter","draft":false,"unlisted":false,"editUrl":"https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/docs/docs/filtering/column-filter/text-filter.md","tags":[],"version":"current","sidebarPosition":1,"frontMatter":{"sidebar_position":1},"sidebar":"tutorialSidebar","previous":{"title":"Column Filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filter/"},"next":{"title":"Number Filter","permalink":"/ag-grid-jpa-adapter/filtering/column-filter/number-filter"}}');var n=r(4848),l=r(8453);const s={sidebar_position:1},a="Text Filter",o={},d=[{value:"Using Text Filter",id:"using-text-filter",level:2},{value:"Text Filter Parameters",id:"text-filter-parameters",level:2},{value:"Text Formatter",id:"text-formatter",level:2},{value:"Text Custom Matcher",id:"text-custom-matcher",level:2},{value:"Text Filter Model",id:"text-filter-model",level:2}];function c(e){const t={a:"a",code:"code",h1:"h1",h2:"h2",header:"header",p:"p",pre:"pre",strong:"strong",table:"table",tbody:"tbody",td:"td",th:"th",thead:"thead",tr:"tr",...(0,l.R)(),...e.components};return(0,n.jsxs)(n.Fragment,{children:[(0,n.jsx)(t.header,{children:(0,n.jsx)(t.h1,{id:"text-filter",children:"Text Filter"})}),"\n",(0,n.jsx)(t.p,{children:"Text Filters allow you to filter string data."}),"\n",(0,n.jsx)(t.h2,{id:"using-text-filter",children:"Using Text Filter"}),"\n",(0,n.jsxs)(t.p,{children:["Text filter is represented by class ",(0,n.jsx)(t.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgTextColumnFilter.java",children:"AgTextColumnFilter"}),"."]}),"\n",(0,n.jsxs)(t.p,{children:["Text Filter is the default filter used in ",(0,n.jsx)(t.code,{children:"ColDef"}),", but it can also be explicitly configured as shown below."]}),"\n",(0,n.jsx)(t.pre,{children:(0,n.jsx)(t.code,{className:"language-java",children:'ColDef colDef = ColDef.builder()\n    .field("portfolio")\n    .filter(new AgTextColumnFilter())\n    .build()\n'})}),"\n",(0,n.jsx)(t.h2,{id:"text-filter-parameters",children:"Text Filter Parameters"}),"\n",(0,n.jsxs)(t.p,{children:["Text Filters are configured though the filter params (",(0,n.jsx)(t.a,{href:"https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/TextFilterParams.java",children:"TextFilterParams"})," class)"]}),"\n",(0,n.jsxs)(t.table,{children:[(0,n.jsx)(t.thead,{children:(0,n.jsxs)(t.tr,{children:[(0,n.jsx)(t.th,{children:"Property"}),(0,n.jsx)(t.th,{children:"Type"}),(0,n.jsx)(t.th,{children:"Default"}),(0,n.jsx)(t.th,{children:"Description"})]})}),(0,n.jsxs)(t.tbody,{children:[(0,n.jsxs)(t.tr,{children:[(0,n.jsx)(t.td,{children:(0,n.jsx)(t.strong,{children:(0,n.jsx)(t.code,{children:"textMatcher"})})}),(0,n.jsx)(t.td,{children:(0,n.jsx)(t.code,{children:"BiFunction<CriteriaBuilder, TextMatcherParams, Predicate>"})}),(0,n.jsx)(t.td,{children:"\u2014"}),(0,n.jsx)(t.td,{children:"Used to override how to filter based on the user input. Returns true if the value passes the filter, otherwise false."})]}),(0,n.jsxs)(t.tr,{children:[(0,n.jsx)(t.td,{children:(0,n.jsx)(t.strong,{children:(0,n.jsx)(t.code,{children:"caseSensitive"})})}),(0,n.jsx)(t.td,{children:(0,n.jsx)(t.code,{children:"boolean"})}),(0,n.jsx)(t.td,{children:(0,n.jsx)(t.code,{children:"false"})}),(0,n.jsx)(t.td,{children:"By default, text filtering is case-insensitive. Set this to true to make text filtering case-sensitive."})]}),(0,n.jsxs)(t.tr,{children:[(0,n.jsx)(t.td,{children:(0,n.jsx)(t.strong,{children:(0,n.jsx)(t.code,{children:"textFormatter"})})}),(0,n.jsx)(t.td,{children:(0,n.jsx)(t.code,{children:"BiFunction<CriteriaBuilder, Expression<String>, Expression<String>>"})}),(0,n.jsx)(t.td,{children:"\u2014"}),(0,n.jsx)(t.td,{children:"Formats the text before applying the filter compare logic. Useful if you want to substitute accented characters, for example."})]}),(0,n.jsxs)(t.tr,{children:[(0,n.jsx)(t.td,{children:(0,n.jsx)(t.strong,{children:(0,n.jsx)(t.code,{children:"trimInput"})})}),(0,n.jsx)(t.td,{children:(0,n.jsx)(t.code,{children:"boolean"})}),(0,n.jsx)(t.td,{children:(0,n.jsx)(t.code,{children:"false"})}),(0,n.jsx)(t.td,{children:"If true, the input that the user enters will be trimmed when the filter is applied, so any leading or trailing whitespace will be removed. If only whitespace is entered, it will be left as-is."})]}),(0,n.jsxs)(t.tr,{children:[(0,n.jsx)(t.td,{children:(0,n.jsx)(t.strong,{children:(0,n.jsx)(t.code,{children:"filterOptions"})})}),(0,n.jsx)(t.td,{children:(0,n.jsx)(t.code,{children:"Set<SimpleFilterModelType>"})}),(0,n.jsx)(t.td,{children:"All available"}),(0,n.jsx)(t.td,{children:"Which filtering operations are allowed."})]})]})]}),"\n",(0,n.jsx)(t.p,{children:"Example of using filter parameters."}),"\n",(0,n.jsx)(t.pre,{children:(0,n.jsx)(t.code,{className:"language-java",children:'ColDef colDef = ColDef.builder()\n    .field("portfolio")\n    .filter(new AgTextColumnFilter()\n        .filterParams(\n            TextFilterParams.builder()\n                .caseSensitive(false)\n                .trimInput(true)\n                .filterOptions(SimpleFilterModelType.contains, SimpleFilterModelType.startsWith)\n                .build()\n        )\n    )\n    .build()\n'})}),"\n",(0,n.jsx)(t.h2,{id:"text-formatter",children:"Text Formatter"}),"\n",(0,n.jsxs)(t.p,{children:["By default, the grid compares the Text Filter with the values in a case-insensitive way, by converting both the filter text and the values to lower case and comparing them; for example, ",(0,n.jsx)(t.code,{children:"'o'"})," will match ",(0,n.jsx)(t.code,{children:"'Olivia'"})," and ",(0,n.jsx)(t.code,{children:"'Salmon'"}),".\nIf you instead want to have case-sensitive matches, you can set ",(0,n.jsx)(t.code,{children:"caseSensitive = true"})," in the ",(0,n.jsx)(t.code,{children:"filterParams"}),", so that no lowercasing is performed.\nIn this case, ",(0,n.jsx)(t.code,{children:"'o'"})," would no longer match ",(0,n.jsx)(t.code,{children:"'Olivia'"}),"."]}),"\n",(0,n.jsxs)(t.p,{children:["You might have more advanced requirements, for example to ignore accented characters.\nIn this case, you can provide your own ",(0,n.jsx)(t.code,{children:"textFormatter"}),", which formats the text before applying the filter compare logic.\n",(0,n.jsx)(t.code,{children:"textFormatter"})," is a function, that takes as argument ",(0,n.jsx)(t.code,{children:"CriteriaBuilder"})," object and ",(0,n.jsx)(t.code,{children:"Expression<String>"})," and returns new ",(0,n.jsx)(t.code,{children:"Expression<String>"})," which is then used\nin the filter compare logic."]}),"\n",(0,n.jsx)(t.pre,{children:(0,n.jsx)(t.code,{className:"language-java",children:'ColDef colDef = ColDef.builder()\n    .field("portfolio")\n    .filter(new AgTextColumnFilter()\n        .filterParams(\n            TextFilterParams.builder()\n                .textFormatter((cb, expr) => {\n                    Expression<String> newExpression = expr;\n                    // lower input\n                    newExpression = cb.lower(newExpression);\n                    // Remove accents\n                    newExpression = cb.function("TRANSLATE", String.class, newExpression,\n                        cb.literal("\xe1\xe9\xed\xf3\xfa\xc1\xc9\xcd\xd3\xda\xfc\xdc\xf1\xd1"),\n                        cb.literal("aeiouAEIOUuUnN"));\n                    \n                    return newExpression;\n                })\n                .build()\n        )\n    )\n    .build()\n'})}),"\n",(0,n.jsxs)(t.p,{children:["Note that when providing a Text Formatter, the ",(0,n.jsx)(t.code,{children:"caseSensitive"})," parameter is ignored.\nIn this situation, if you want to do a case-insensitive comparison, you will need to perform case conversion inside the ",(0,n.jsx)(t.code,{children:"textFormatter"})," function."]}),"\n",(0,n.jsx)(t.h2,{id:"text-custom-matcher",children:"Text Custom Matcher"}),"\n",(0,n.jsx)(t.h2,{id:"text-filter-model",children:"Text Filter Model"})]})}function h(e={}){const{wrapper:t}={...(0,l.R)(),...e.components};return t?(0,n.jsx)(t,{...e,children:(0,n.jsx)(c,{...e})}):c(e)}},8453:(e,t,r)=>{r.d(t,{R:()=>s,x:()=>a});var i=r(6540);const n={},l=i.createContext(n);function s(e){const t=i.useContext(l);return i.useMemo((function(){return"function"==typeof e?e(t):{...t,...e}}),[t,e])}function a(e){let t;return t=e.disableParentContext?"function"==typeof e.components?e.components(n):e.components||n:s(e.components),i.createElement(l.Provider,{value:t},e.children)}}}]);
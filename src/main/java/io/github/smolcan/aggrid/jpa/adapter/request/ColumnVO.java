package io.github.smolcan.aggrid.jpa.adapter.request;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnVO {
    private String id;
    private String displayName;
    private String field;
    private String aggFunc;
}
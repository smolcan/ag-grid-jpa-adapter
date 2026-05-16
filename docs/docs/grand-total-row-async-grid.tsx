import {GRAND_TOTAL_ROW_ID, ModuleRegistry} from 'ag-grid-community';
import {
    ColDef,
    ColumnAutoSizeModule,
    EventApiModule, GridReadyEvent, IServerSideDatasource, IServerSideGetRowsParams,
    NumberFilterModule, RowGroupingPanelModule, ServerSideRowModelApiModule,
    ServerSideRowModelModule, SetFilterModule, SetFilterParams,
    TextFilterModule, themeQuartz,
    ValidationModule
} from 'ag-grid-enterprise';
import React, {useMemo, useState} from 'react';
import {useColorMode} from '@docusaurus/theme-common';
import {AgGridReact} from 'ag-grid-react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';


ModuleRegistry.registerModules([ServerSideRowModelModule, NumberFilterModule, TextFilterModule, ValidationModule, ColumnAutoSizeModule, EventApiModule, RowGroupingPanelModule, SetFilterModule, ServerSideRowModelApiModule]);

const GrandTotalRowAsyncGrid = () => {
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const { siteConfig } = useDocusaurusContext();
    const { API_URL } = siteConfig.customFields;

    const { colorMode } = useColorMode();

    const theme = useMemo(() =>
        themeQuartz.withParams({
            backgroundColor: colorMode === 'dark' ? "#1f2836" : "#ffffff",
            browserColorScheme: colorMode,
            chromeBackgroundColor: {
                ref: "foregroundColor",
                mix: 0.07,
                onto: "backgroundColor"
            },
            foregroundColor: colorMode === 'dark' ? "#FFF" : "#000",
            headerFontSize: 14
        }), [colorMode]);

    const columnDefs = useMemo(() => [
        {
            headerName: 'Book',
            field: 'book',
            cellDataType: 'text',
            filter: 'agSetColumnFilter',
            floatingFilter: true,
            filterParams: {
                values: params => {
                    const field = params.colDef.field;
                    fetch(`${API_URL}/docs/grand-total-row/async/supplySetFilterValues/${field}`)
                        .then(async response => {
                            if (!response.ok) {
                                throw new Error(await response.text() || `HTTP error! status: ${response.status}`);
                            }
                            return response.json();
                        })
                        .then(data => params.success(data.data))
                        .catch(error => {
                            console.error('Error fetching data:', error);
                            setErrorMessage(error.message || 'Failed to fetch data');
                        });
                }
            } as SetFilterParams,
        },
        {
            headerName: 'Current Value',
            field: 'currentValue',
            cellDataType: 'number',
            filter: 'agNumberColumnFilter',
            floatingFilter: true,
            aggFunc: 'avg',
        },
        {
            headerName: 'Previous Value',
            field: 'previousValue',
            cellDataType: 'number',
            filter: 'agNumberColumnFilter',
            floatingFilter: true,
            aggFunc: 'sum',
        },
    ] as ColDef[], []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: true,
        flex: 1,
    } as ColDef), []);

    const postJson = (url: string, body: unknown) =>
        fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        }).then(async response => {
            if (!response.ok) {
                throw new Error(await response.text() || `HTTP error! status: ${response.status}`);
            }
            return response.json();
        });

    const refreshGrandTotal = (params: IServerSideGetRowsParams) => {
        const { api, request } = params;
        // Clear any stale total immediately so the user doesn't see outdated aggregates
        // while the new fetch is in flight.
        api.applyServerSideTransaction({
            remove: [{ tradeId: GRAND_TOTAL_ROW_ID } as any],
        });

        postJson(`${API_URL}/docs/grand-total-row/async/getGrandTotalData`, request)
            .then(data => {
                setErrorMessage(null);
                const grandTotalData = { ...data.data, tradeId: GRAND_TOTAL_ROW_ID };
                // addIndex is required because the backend doesn't return rowCount, leaving
                // the store's isLastRowKnown=false. Without an explicit index, insertRowNodes
                // early-returns before reaching the GRAND_TOTAL_ROW_ID branch.
                api.applyServerSideTransaction({ add: [grandTotalData], addIndex: 0 });
            })
            .catch(error => {
                console.error('Error fetching data:', error);
                setErrorMessage(error.message || 'Failed to fetch data');
            });
    };

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            const needsGrandTotal = params.needsGrandTotal;
            postJson(`${API_URL}/docs/grand-total-row/async/getRows`, params.request)
                .then(data => {
                    setErrorMessage(null);
                    // The sync endpoint returns grandTotalData: null on this response;
                    // forwarding null to params.success would tell the grid to remove the
                    // grand total. We own it via transaction here, so strip it.
                    const { grandTotalData: _omit, ...successData } = data.data;
                    params.success(successData);
                    if (needsGrandTotal) {
                        refreshGrandTotal(params);
                    }
                })
                .catch(error => {
                    console.error('Error fetching data:', error);
                    setErrorMessage(error.message || 'Failed to fetch data');
                    params.fail();
                });
        }
    }), []);

    const onGridReady = (params: GridReadyEvent) => params.api.sizeColumnsToFit();

    return (
        <div style={{
            backgroundColor: colorMode == 'dark' ? '#1a1c1d' : '#ffffff',
            marginBottom: '1rem',
            borderRadius: '8px',
            fontFamily: 'system-ui, -apple-system, sans-serif',
            padding: '1rem'
        }}>
            {errorMessage && (
                <div style={{
                    backgroundColor: '#ff4d4f',
                    color: '#fff',
                    display: 'inline-block',
                    padding: '0.5rem 1rem',
                    borderRadius: '20px',
                    fontSize: '0.875rem',
                    marginBottom: '1rem',
                    fontWeight: 500
                }}>
                    {errorMessage}
                </div>
            )}
            <div style={{ height: '500px', width: '100%' }}>
                <AgGridReact
                    columnDefs={columnDefs}
                    defaultColDef={defaultColDef}
                    serverSideDatasource={serverSideDatasource}
                    onGridReady={onGridReady}
                    rowModelType="serverSide"
                    theme={theme}
                    animateRows={true}
                    suppressMenuHide={true}
                    rowGroupPanelShow={'always'}
                    getRowId={(params) => params.data.tradeId + ''}
                    grandTotalRow={'bottom'}
                />
            </div>
        </div>
    );
};

export default GrandTotalRowAsyncGrid;

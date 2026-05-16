import {GRAND_TOTAL_ROW_ID, ModuleRegistry} from 'ag-grid-community';
import {
    ColDef,
    ColumnAutoSizeModule,
    EventApiModule, GridApi, GridReadyEvent, IServerSideDatasource,
    NumberFilterModule, RowGroupingPanelModule, ServerSideRowModelApiModule,
    ServerSideRowModelModule, SetFilterModule, SetFilterParams,
    TextFilterModule, themeQuartz,
    ValidationModule
} from 'ag-grid-enterprise';
import React, {useCallback, useMemo, useRef, useState} from 'react';
import {useColorMode} from '@docusaurus/theme-common';
import {AgGridReact} from 'ag-grid-react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';


ModuleRegistry.registerModules([ServerSideRowModelModule, NumberFilterModule, TextFilterModule, ValidationModule, ColumnAutoSizeModule, EventApiModule, RowGroupingPanelModule, SetFilterModule, ServerSideRowModelApiModule]);

const GrandTotalRowAsyncGrid = () => {
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const gridApiRef = useRef<GridApi | null>(null);
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
                    fetch(`${API_URL}/docs/grand-total-row/async/supplySetFilterValues/${field}`, {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                    })
                        .then(async response => {
                            if (!response.ok) {
                                const errorText = await response.text(); // Read plain text from Spring Boot
                                throw new Error(errorText || `HTTP error! status: ${response.status}`);
                            }
                            return response.json();
                        })
                        .then(data => {
                            params.success(data.data);
                        })
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

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            fetch(`${API_URL}/docs/grand-total-row/async/getRows`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(params.request)
            })
                .then(async response => {
                    if (!response.ok) {
                        const errorText = await response.text(); // Read plain text from Spring Boot
                        throw new Error(errorText || `HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    setErrorMessage(null);
                    const successData = data.data;
                    successData.grandTotalData = undefined;
                    params.success(successData);
                })
                .catch(error => {
                    console.error('Error fetching data:', error);
                    setErrorMessage(error.message || 'Failed to fetch data');
                    params.fail();
                });
            
            if (params.needsGrandTotal) {
                const { api, request } = params;
                api.applyServerSideTransaction({
                    remove: [{ tradeId: GRAND_TOTAL_ROW_ID } as any],
                });

                fetch(`${API_URL}/docs/grand-total-row/async/getGrandTotalData`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(request)
                })
                .then(async response => {
                    if (!response.ok) {
                        const errorText = await response.text(); // Read plain text from Spring Boot
                        throw new Error(errorText || `HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    setErrorMessage(null);
                    
                    const grandTotalData = {
                        ...data.data,
                        tradeId: GRAND_TOTAL_ROW_ID,
                    }
                    api.applyServerSideTransaction({ add: [grandTotalData] });
                })
                .catch(error => {
                    console.error('Error fetching data:', error);
                    setErrorMessage(error.message || 'Failed to fetch data');
                });
            }
        }
    }), []);

    const onGridReady = useCallback((params: GridReadyEvent) => {
        gridApiRef.current = params.api;
        params.api.sizeColumnsToFit();
    }, []);

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

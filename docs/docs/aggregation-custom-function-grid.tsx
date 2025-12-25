import React, { useMemo, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry } from 'ag-grid-community';
import {
    ColDef, ColumnAutoSizeModule, ColumnsToolPanelModule, DateFilterModule, FiltersToolPanelModule,
    GridReadyEvent,
    IServerSideDatasource, NumberFilterModule, RowGroupingPanelModule,
    ServerSideRowModelModule, SideBarModule, TextFilterModule,
    themeQuartz, ValidationModule
} from 'ag-grid-enterprise';
import { useColorMode } from '@docusaurus/theme-common';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

// Register the required modules
ModuleRegistry.registerModules([ServerSideRowModelModule, ValidationModule, ColumnAutoSizeModule, NumberFilterModule, TextFilterModule, RowGroupingPanelModule, DateFilterModule, SideBarModule, ColumnsToolPanelModule, FiltersToolPanelModule]);

const AggregationCustomFunctionGrid = () => {
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const { colorMode } = useColorMode();
    const { siteConfig } = useDocusaurusContext();
    const { API_URL } = siteConfig.customFields;

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
            headerName: 'Product',
            field: 'product',
            enableRowGroup: true,
            rowGroup: true,
            hide: true,
            cellDataType: 'text'
        },
        {
            headerName: 'Portfolio',
            field: 'portfolio',
            enableRowGroup: true,
            rowGroup: true,
            hide: true,
            cellDataType: 'text'
        },
        {
            headerName: 'Book',
            field: 'book',
            enableRowGroup: true,
            rowGroup: true,
            hide: true,
            cellDataType: 'text'
        },
        {
            headerName: 'Current Value',
            field: 'currentValue',
            cellDataType: 'number',
            initialAggFunc: 'stddev_pop',
            filter: 'agNumberColumnFilter'
        },
        {
            headerName: 'Previous Value',
            field: 'previousValue',
            cellDataType: 'number',
            initialAggFunc: 'stddev_samp',
            filter: 'agNumberColumnFilter'
        },
        {
            headerName: 'Is Sold',
            field: 'isSold',
            cellDataType: 'boolean',
            initialAggFunc: 'bool_and',
            filter: false,
        },
    ] as ColDef[], []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: false,
        flex: 1,
    } as ColDef), []);

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            fetch(`${API_URL}/docs/aggregation/custom-agg-func/getRows`, {
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
                    params.success(data);
                })
                .catch(error => {
                    console.error('Error fetching data:', error);
                    setErrorMessage(error.message || 'Failed to fetch data');
                    params.fail();
                });
        }
    }), []);

    const onGridReady = (params: GridReadyEvent) => {
        params.api.sizeColumnsToFit();
        params.api.addAggFuncs({
            'bool_and': p => {},    // leave empty, just register, server will handle
            'stddev_pop': p => {},
            'stddev_samp': p => {},
        });
    };

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
                    sideBar={true}
                />
            </div>
        </div>
    );
};

export default AggregationCustomFunctionGrid;
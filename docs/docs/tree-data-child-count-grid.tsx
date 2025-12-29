import React, { useMemo, useState } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry } from 'ag-grid-community';
import {
    ColDef, ColumnAutoSizeModule,
    GridReadyEvent,
    IServerSideDatasource,
    ServerSideRowModelModule,
    themeQuartz, TreeDataModule, ValidationModule
} from 'ag-grid-enterprise';
import { useColorMode } from '@docusaurus/theme-common';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

ModuleRegistry.registerModules([ServerSideRowModelModule, ValidationModule, ColumnAutoSizeModule, TreeDataModule]);

const TreeDataChildCountGrid = () => {

    const { siteConfig } = useDocusaurusContext();
    const { API_URL } = siteConfig.customFields;

    // const API_URL = process.env.REACT_APP_API_URL;
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
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
            headerName: 'Trade ID',
            field: 'tradeId',
            cellDataType: 'number',
        },
        {
            headerName: 'Product',
            field: 'product',
            cellDataType: 'text'
        },
        {
            headerName: 'Portfolio',
            field: 'portfolio',
            cellDataType: 'text'
        },
        {
            headerName: 'Current Value',
            field: 'currentValue',
            cellDataType: 'number',
        },
        {
            headerName: 'Previous Value',
            field: 'previousValue',
            cellDataType: 'number', 
        },
        
    ] as ColDef[], []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: false,
        flex: 1,
    } as ColDef), []);

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            fetch(`${API_URL}/docs/tree-data/child-count/getRows`, {
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
                    params.success(data.data);
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
                    treeData={true}
                    isServerSideGroup={(dataItem) => {
                        return dataItem['hasChildren'];
                    }}
                    getServerSideGroupKey={(dataItem) => {
                        return dataItem['tradeId'];
                    }}
                    onGridReady={onGridReady}
                    rowModelType="serverSide"
                    theme={theme}
                    animateRows={true}
                    suppressMenuHide={true}
                    getChildCount={(dataItem) => dataItem.childCount}
                />
            </div>
        </div>
    );
};

export default TreeDataChildCountGrid;
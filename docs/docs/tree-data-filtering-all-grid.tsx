import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry } from 'ag-grid-community';
import {
    AdvancedFilterModule,
    ColDef, ColumnAutoSizeModule,
    GridReadyEvent,
    IServerSideDatasource, NumberFilter, NumberFilterModule,
    ServerSideRowModelModule, TextFilterModule,
    themeQuartz, TreeDataModule, ValidationModule
} from 'ag-grid-enterprise';
import { useColorMode } from '@docusaurus/theme-common';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import {useDebounce} from 'use-debounce';

ModuleRegistry.registerModules([ServerSideRowModelModule, ValidationModule, ColumnAutoSizeModule, TreeDataModule, AdvancedFilterModule]);

const TreeDataFilteringAllGrid = () => {

    const { siteConfig } = useDocusaurusContext();
    const { API_URL } = siteConfig.customFields;
    const gridRef = useRef<AgGridReact>(null);
    
    const [quickFilterValue, setQuickFilterValue] = useState('');
    const [debouncedQuickFilter] = useDebounce(quickFilterValue, 300);
    const filterRef = useRef('');
    useEffect(() => {
        filterRef.current = debouncedQuickFilter;
        if (gridRef?.current?.api) {
            gridRef.current.api.onFilterChanged();
        }
    }, [debouncedQuickFilter]);
    
    // const API_URL = process.env.REACT_APP_API_URL;
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const { colorMode } = useColorMode();

    let externalFilterValue = "Everything";
    const externalFilterChanged = useCallback((newValue: string) => {
        externalFilterValue = newValue;
        gridRef.current!.api.onFilterChanged();
    }, []);

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
            cellDataType: 'text',
        },
        {
            headerName: 'Portfolio',
            field: 'portfolio',
            cellDataType: 'text',
        },
        {
            headerName: 'Data Path',
            field: 'dataPath',
            cellDataType: 'text',
        },
        
    ] as ColDef[], []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: true,
        flex: 1,
    } as ColDef), []);

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            fetch(`${API_URL}/docs/tree-data/filtering/all/getRows`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    ...params.request,
                    externalFilter: externalFilterValue,
                    quickFilter: filterRef.current,
                })
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

    const onFilterTextBoxChanged = useCallback(() => {
        const value = (document.getElementById("filter-text-box") as HTMLInputElement).value;
        setQuickFilterValue(value)
    }, []);

    return (
        <div style={{
            backgroundColor: colorMode == 'dark' ? '#1a1c1d' : '#ffffff',
            marginBottom: '1rem',
            borderRadius: '8px',
            fontFamily: 'system-ui, -apple-system, sans-serif',
            padding: '1rem'
        }}>

            <div className="example-header" style={{marginBottom: '10px'}}>
                <span style={{marginRight: '10px'}}>Quick Filter:</span>
                <input
                    type="text"
                    id="filter-text-box"
                    placeholder="Filter..."
                    onInput={onFilterTextBoxChanged}
                    style={{padding: '5px', borderRadius: '4px', border: '1px solid #ccc'}}
                />
            </div>

            <div className="test-container">
                <div className="test-header">
                    <label>
                        <input
                            type="radio"
                            name="filter"
                            id="everything"
                            onChange={() => externalFilterChanged("Everything")}
                        />
                        Everything
                    </label>
                    <label>
                        <input
                            type="radio"
                            name="filter"
                            id="Trade Id Odd"
                            onChange={() => externalFilterChanged("Trade Id Odd")}
                        />
                        Trade Id Odd
                    </label>
                    <label>
                        <input
                            type="radio"
                            name="filter"
                            id="Trade Id Even"
                            onChange={() => externalFilterChanged("Trade Id Even")}
                        />
                        Trade Id Even
                    </label>
                </div>
            </div>
            
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
                    ref={gridRef}
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
                    enableAdvancedFilter={true}
                    onGridReady={onGridReady}
                    rowModelType="serverSide"
                    theme={theme}
                    animateRows={true}
                    suppressMenuHide={true}
                />
            </div>
        </div>
    );
};

export default TreeDataFilteringAllGrid;
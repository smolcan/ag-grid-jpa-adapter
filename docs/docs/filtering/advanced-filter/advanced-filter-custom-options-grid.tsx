import React, {useMemo, useState} from 'react';
import {useColorMode} from '@docusaurus/theme-common';
import CodeBlock from '@theme/CodeBlock';
import {
    AdvancedFilterModule,
    ColDef, ColumnAutoSizeModule,
    DateFilterParams,
    GridReadyEvent,
    IServerSideDatasource,
    NumberFilterParams,
    ServerSideRowModelModule,
    TextFilterParams,
    themeQuartz, ValidationModule
} from 'ag-grid-enterprise';
import {AgGridReact} from 'ag-grid-react';
import { ModuleRegistry } from 'ag-grid-community';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

ModuleRegistry.registerModules([ ServerSideRowModelModule, AdvancedFilterModule, ValidationModule, ColumnAutoSizeModule ]);

// required by AG Grid, but it only runs with the Client-Side Row Model, the server evaluates the option here
const serverSideOnly = () => true;

const AdvancedFilterCustomOptionsGrid = () => {
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [lastFilterModel, setLastFilterModel] = useState<any>(null);
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
            headerName: 'Trade Id',
            field: 'tradeId',
            cellDataType: 'number',
            filter: false,
        },
        {
            headerName: 'Portfolio',
            field: 'portfolio',
            cellDataType: 'text',
            filterParams: {
                filterOptions: [
                    'contains',
                    'equals',
                    {
                        displayKey: 'startsWithVowel',
                        displayName: 'Starts with vowel',
                        numberOfInputs: 0,
                        predicate: serverSideOnly,
                    },
                ],
            } as TextFilterParams,
        },
        {
            headerName: 'Current Value',
            field: 'currentValue',
            cellDataType: 'number',
            filterParams: {
                filterOptions: [
                    'equals',
                    'lessThan',
                    'greaterThan',
                    'inRange',
                    {
                        displayKey: 'betweenExclusive',
                        displayName: 'Between (Exclusive)',
                        numberOfInputs: 2,
                        predicate: serverSideOnly,
                    },
                ],
            } as NumberFilterParams,
        },
        {
            headerName: 'Birth Date',
            field: 'birthDate',
            cellDataType: 'dateString',
            filterParams: {
                filterOptions: [
                    'equals',
                    'lessThan',
                    'greaterThan',
                    {
                        displayKey: 'sameYearAs',
                        displayName: 'Same year as',
                        numberOfInputs: 1,
                        predicate: serverSideOnly,
                    },
                ],
            } as DateFilterParams,
        },
    ] as ColDef[], []);

    const defaultColDef = useMemo(() => ({
        resizable: true,
        filter: true,
        flex: 1,
    } as ColDef), []);

    const serverSideDatasource: IServerSideDatasource = useMemo(() => ({
        getRows: (params) => {
            setLastFilterModel(params.request.filterModel);
            fetch(`${API_URL}/docs/filtering/advanced-filter/custom-filter-options/getRows`, {
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

export default AdvancedFilterCustomOptionsGrid;

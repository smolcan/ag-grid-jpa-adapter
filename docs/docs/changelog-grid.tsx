import React, { useMemo } from 'react';
import { AgGridReact } from 'ag-grid-react';
import { ModuleRegistry } from 'ag-grid-community';
import {
    ClientSideRowModelModule,
    ColDef, FirstDataRenderedEvent, ICellRendererParams, IDetailCellRendererParams,
    MasterDetailModule, RowApiModule, RowAutoHeightModule, themeQuartz, ValidationModule
} from 'ag-grid-enterprise';
import { useColorMode } from '@docusaurus/theme-common';
import Link from '@docusaurus/Link';
import changelog from './changelog.json';

ModuleRegistry.registerModules([ClientSideRowModelModule, ValidationModule, MasterDetailModule, RowApiModule, RowAutoHeightModule]);

const REPOSITORY = 'https://github.com/smolcan/ag-grid-jpa-adapter';
const TAG_PREFIX = 'ag-grid-jpa-adapter-';
// the release before the first one listed, for its compare link
const PREVIOUS_RELEASE = '0.0.54';

type ChangeType = 'Added' | 'Changed' | 'Fixed' | 'Deprecated' | 'Breaking';

interface Change {
    type: ChangeType;
    description: string;
    commit: string;
    // docs route, without the base url
    docs?: string;
}

interface Release {
    version: string;
    date: string;
    changes: Change[];
    previousVersion: string;
}

const TYPE_ORDER: ChangeType[] = ['Breaking', 'Added', 'Changed', 'Fixed', 'Deprecated'];

const TYPE_COLORS: Record<ChangeType, string> = {
    Breaking: '#d4380d',
    Added: '#389e0d',
    Changed: '#1677ff',
    Fixed: '#7c3aed',
    Deprecated: '#ad6800',
};

const releases: Release[] = (changelog as Omit<Release, 'previousVersion'>[]).map((release, i, all) => ({
    ...release,
    previousVersion: i + 1 < all.length ? all[i + 1].version : PREVIOUS_RELEASE,
}));

const TypeBadge = ({ type, count }: { type: ChangeType, count?: number }) => (
    <span style={{
        backgroundColor: TYPE_COLORS[type],
        color: '#fff',
        borderRadius: '10px',
        padding: '2px 8px',
        marginRight: '6px',
        fontSize: '0.75rem',
        fontWeight: 600,
    }}>
        {count === undefined ? type : `${count} ${type}`}
    </span>
);

// renders `backticked` parts of the description as code
const Description = ({ value }: { value: string }) => (
    <div style={{ lineHeight: 1.6, padding: '10px 0' }}>
        {value.split('`').map((part, i) => i % 2 === 1 ? <code key={i}>{part}</code> : part)}
    </div>
);

const ChangelogGrid = () => {
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
            field: 'version',
            headerName: 'Version',
            cellRenderer: 'agGroupCellRenderer',
            width: 115,
        },
        {
            field: 'date',
            headerName: 'Released',
            width: 120,
        },
        {
            headerName: 'Changes',
            flex: 1,
            minWidth: 300,
            valueGetter: params => params.data.changes.length,
            cellRenderer: (params: ICellRendererParams<Release>) => TYPE_ORDER
                .map(type => ({ type, count: params.data.changes.filter(c => c.type === type).length }))
                .filter(({ count }) => count > 0)
                .map(({ type, count }) => <TypeBadge key={type} type={type} count={count} />),
        },
        {
            headerName: 'Diff',
            width: 140,
            sortable: false,
            cellRenderer: (params: ICellRendererParams<Release>) => (
                <a href={`${REPOSITORY}/compare/${TAG_PREFIX}${params.data.previousVersion}...${TAG_PREFIX}${params.data.version}`}
                   target="_blank" rel="noopener noreferrer">
                    {params.data.previousVersion}...{params.data.version}
                </a>
            ),
        },
    ] as ColDef<Release>[], []);

    const detailCellRendererParams = useMemo(() => ({
        detailGridOptions: {
            columnDefs: [
                {
                    field: 'type',
                    headerName: 'Type',
                    width: 130,
                    cellRenderer: (params: ICellRendererParams<Change>) => <TypeBadge type={params.value} />,
                },
                {
                    field: 'description',
                    headerName: 'Change',
                    flex: 1,
                    wrapText: true,
                    autoHeight: true,
                    cellRenderer: (params: ICellRendererParams<Change>) => <Description value={params.value} />,
                },
                {
                    field: 'docs',
                    headerName: 'Docs',
                    width: 90,
                    cellRenderer: (params: ICellRendererParams<Change>) => params.value
                        ? <Link to={params.value}>Docs</Link>
                        : null,
                },
                {
                    field: 'commit',
                    headerName: 'Commit',
                    width: 110,
                    cellRenderer: (params: ICellRendererParams<Change>) => (
                        <a href={`${REPOSITORY}/commit/${params.value}`} target="_blank" rel="noopener noreferrer">
                            <code>{params.value}</code>
                        </a>
                    ),
                },
            ] as ColDef<Change>[],
            defaultColDef: { sortable: false, resizable: true },
            domLayout: 'autoHeight',
        },
        getDetailRowData: params => params.successCallback(params.data.changes),
    } as Partial<IDetailCellRendererParams<Release, Change>>), []);

    // opens the latest release
    const onFirstDataRendered = (params: FirstDataRenderedEvent<Release>) => {
        params.api.getDisplayedRowAtIndex(0)?.setExpanded(true);
    };

    return (
        <div style={{ marginBottom: '1rem' }}>
            <AgGridReact<Release>
                rowData={releases}
                columnDefs={columnDefs}
                getRowId={params => params.data.version}
                masterDetail={true}
                detailCellRendererParams={detailCellRendererParams}
                detailRowAutoHeight={true}
                onFirstDataRendered={onFirstDataRendered}
                domLayout="autoHeight"
                animateRows={false}
                theme={theme}
            />
        </div>
    );
};

export default ChangelogGrid;

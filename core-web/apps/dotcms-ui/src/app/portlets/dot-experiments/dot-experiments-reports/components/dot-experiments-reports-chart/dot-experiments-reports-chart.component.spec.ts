import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { ChartModule, UIChart } from 'primeng/chart';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';
import {
    CHARTJS_DATA_MOCK_EMPTY,
    CHARTJS_DATA_MOCK_WITH_DATA
} from '@portlets/dot-experiments/test/mocks';
import { DotMessagePipe } from '@tests/dot-message-mock.pipe';

import { DotExperimentsReportsChartComponent } from './dot-experiments-reports-chart.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.reports.chart.title': 'title',
    'experiments.reports.chart.empty.title': 'x axis label',
    'experiments.reports.chart.empty.description': 'y axis label'
});
describe('DotExperimentsReportsChartComponent', () => {
    let spectator: Spectator<DotExperimentsReportsChartComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsReportsChartComponent,
        imports: [ChartModule, DotMessagePipe],
        componentProviders: [],
        declarations: [],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should has title, legends container and PrimeNG Chart Component', () => {
        spectator.setInput({
            loading: false,
            data: CHARTJS_DATA_MOCK_WITH_DATA,
            config: {
                xAxisLabel: 'experiments.chart.xAxisLabel',
                yAxisLabel: 'experiments.chart.yAxisLabel',
                title: 'experiments.reports.chart.title'
            }
        });

        expect(spectator.query(byTestId('chart-title'))).toContainText('title');
        expect(spectator.query(byTestId('chart-legends'))).toExist();
        expect(spectator.query(UIChart)).toExist();
    });

    it('should show the loading state', () => {
        spectator.setInput({
            loading: true
        });
        expect(spectator.query(byTestId('loading-skeleton'))).toExist();
    });
    it('should show the empty state', () => {
        spectator.setInput({
            loading: false,
            data: CHARTJS_DATA_MOCK_EMPTY,
            config: {
                xAxisLabel: 'experiments.chart.xAxisLabel',
                yAxisLabel: 'experiments.chart.yAxisLabel',
                title: 'experiments.reports.chart.title'
            }
        });

        expect(spectator.query(byTestId('empty-data-msg'))).toExist();
    });
});
3;

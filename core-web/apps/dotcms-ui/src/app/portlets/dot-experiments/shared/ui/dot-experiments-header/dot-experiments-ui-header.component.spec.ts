import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { Skeleton } from 'primeng/skeleton';
import { Tag, TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatusList } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsUiHeaderComponent } from './dot-experiments-ui-header.component';

const messageServiceMock = new MockDotMessageService({
    running: 'RUNNING'
});
describe('ExperimentsHeaderComponent', () => {
    let spectator: Spectator<DotExperimentsUiHeaderComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsUiHeaderComponent,
        imports: [TagModule],
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

    it('should has a title rendered', () => {
        const title = 'My title';
        spectator.setInput('title', title);
        expect(spectator.query(byTestId('title'))).toHaveText(title);
    });

    it('should emit goBack output when icon is clicked. ', () => {
        let output;
        spectator.output('goBack').subscribe((result) => (output = result));
        const goBackButton = spectator.query(byTestId('goback-link')) as HTMLAnchorElement;
        spectator.click(goBackButton);
        expect(output).toBeTrue();
    });

    it('should show the skeleton component if isLoading true ', () => {
        spectator.setInput({
            isLoading: true
        });

        expect(spectator.query(Skeleton)).toExist();
    });

    it('should rendered the status Input', () => {
        const expectedStatus: DotExperimentStatusList = DotExperimentStatusList.RUNNING;
        spectator.setInput({
            status: DotExperimentStatusList.RUNNING
        });

        expect(spectator.query(Tag)).toExist();
        expect(spectator.query(byTestId('status-tag'))).toContainText(expectedStatus);
    });
});

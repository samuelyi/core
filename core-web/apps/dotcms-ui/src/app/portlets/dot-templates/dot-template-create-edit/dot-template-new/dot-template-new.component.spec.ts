/* eslint-disable @typescript-eslint/no-explicit-any */

import { Subject } from 'rxjs';

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogService } from 'primeng/dynamicdialog';

import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTemplateNewComponent } from './dot-template-new.component';

describe('DotTemplateNewComponent', () => {
    let fixture: ComponentFixture<DotTemplateNewComponent>;
    let dialogService: DialogService;
    let dotRouterService: DotRouterService;

    const dialogRefClose = new Subject();

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTemplateNewComponent],
            providers: [
                DialogService,
                {
                    provide: DotRouterService,
                    useValue: {
                        gotoPortlet: jasmine.createSpy(),
                        goToURL: jasmine.createSpy()
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'templates.select.template.title': 'Create a template'
                    })
                }
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateNewComponent);

        dotRouterService = TestBed.inject(DotRouterService);

        dialogService = TestBed.inject(DialogService);
        spyOn<any>(dialogService, 'open').and.returnValue({
            onClose: dialogRefClose
        });

        fixture.detectChanges();
    });

    it('should open template type selector', () => {
        expect(dialogService.open).toHaveBeenCalledWith(jasmine.any(Function), {
            header: 'Create a template',
            width: '37rem'
        });
    });

    it('should go to create design template', () => {
        dialogRefClose.next('design');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledOnceWith('/templates/new/design');
    });

    it('should go to listing if close the dialog', () => {
        dialogRefClose.next(undefined);
        expect(dotRouterService.goToURL).toHaveBeenCalledOnceWith('/templates');
    });
});

import { ErrorMessageService } from '../../../services/error-message.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog.component';
import {
  Component,
  DestroyRef,
  OnInit,
  inject,
  ChangeDetectionStrategy,
  TemplateRef,
  ViewChild,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
  FormsModule,
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import {
  MatPaginatorIntl,
  MatPaginatorModule,
  PageEvent,
} from '@angular/material/paginator';
import { createMaterialPaginatorIntl } from '../../shared/material-paginator-intl';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Category } from '../../../models/category.model';
import { CatalogService } from '../../../services/catalog.service';

@Component({
  selector: 'app-category-management',
  providers: [
    { provide: MatPaginatorIntl, useFactory: createMaterialPaginatorIntl },
  ],
  styleUrl: './category-management.component.scss',
  imports: [
    TranslatePipe,
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  private readonly errorMessages = inject(ErrorMessageService);
  private readonly translate = inject(TranslateService);
  private confirmationOpen = false;

  private readonly formBuilder = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  @ViewChild('editor') private editor!: TemplateRef<unknown>;
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private loadRevision = 0;
  private editorRef?: MatDialogRef<unknown>;
  constructor() {
    this.destroyRef.onDestroy(() => {
      this.editorRef?.close();
    });
  }

  readonly columns = ['id', 'name', 'actions'];

  changePage(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.load(event.pageIndex);
  }

  private showEditor(): void {
    if (this.saving || this.editorRef) {
      return;
    }
    const ref = this.dialog.open(this.editor, {
      width: '520px',
      maxWidth: 'calc(100vw - 32px)',
      disableClose: true,
    });
    this.editorRef = ref;
    ref
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.editorRef === ref) {
          this.editorRef = undefined;
        }
      });
  }

  formError = '';
  message = '';

  openCreate(): void {
    if (this.saving) {
      return;
    }
    this.cancelEdit();
    this.showEditor();
  }

  categories: Category[] = [];
  error = '';
  loading = false;
  saving = false;
  editingId?: number;
  page = 0;
  pageSize = 10;
  totalElements = 0;
  keyword = '';
  form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.load();
  }

  create(): void {
    if (this.saving) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.formError = '';
    const name = this.form.controls.name.value.trim();
    const request = this.editingId
      ? this.catalog.updateCategory(this.editingId, name)
      : this.catalog.createCategory(name);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving = false;
        this.message = this.editingId
          ? this.translate.instant('admin.categoryUpdated')
          : this.translate.instant('admin.categoryAdded');
        this.cancelEdit();
        this.load(this.page);
      },
      error: (error) => {
        this.saving = false;
        this.formError = this.errorMessages.message(
          error,
          'admin.cannotSaveCategoryCheckForADuplicateOrInvalid',
        );
      },
    });
  }

  edit(category: Category): void {
    if (this.saving) {
      return;
    }
    this.formError = '';
    this.editingId = category.id;
    this.form.setValue({ name: category.name });
    this.showEditor();
  }

  cancelEdit(): void {
    if (this.saving) {
      return;
    }
    this.editorRef?.close();
    this.editorRef = undefined;
    this.formError = '';
    this.editingId = undefined;
    this.form.reset();
  }

  private deleteConfirmed(category: Category): void {
    this.catalog.deleteCategory(category.id).subscribe({
      next: () => this.load(this.page),
      error: (error) => {
        this.error = this.errorMessages.message(
          error,
          'admin.cannotDeleteACategoryContainingProducts',
        );
      },
    });
  }

  load(page = 0): void {
    const revision = ++this.loadRevision;
    this.loading = true;
    this.catalog
      .categoryPage(this.keyword.trim(), page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (revision !== this.loadRevision) {
            return;
          }
          if (response.totalPages > 0 && page >= response.totalPages) {
            this.load(response.totalPages - 1);
            return;
          }
          this.categories = response.content;
          this.page = response.number;
          this.totalElements = response.totalElements;
          this.loading = false;
          this.error = '';
        },
        error: (error) => {
          if (revision !== this.loadRevision) {
            return;
          }
          this.loading = false;
          this.error = this.errorMessages.message(
            error,
            'admin.cannotLoadCategories',
          );
        },
      });
  }

  delete(category: Category): void {
    if (this.confirmationOpen) {
      return;
    }
    this.confirmationOpen = true;
    this.dialog
      .open(ConfirmationDialogComponent, {
        data: this.translate.instant('admin.deleteCategory', {
          p0: category.name,
        }),
        width: '440px',
        maxWidth: 'calc(100vw - 32px)',
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed: boolean | undefined) => {
        this.confirmationOpen = false;
        if (confirmed === true) {
          this.deleteConfirmed(category);
        }
      });
  }
}

"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { useCreateGradeCategory, useDeleteGradeCategory, useGradeCategories } from "@/hooks/useGradeCategories";

const categorySchema = z.object({
  name: z.string().min(1, "Name is required"),
  weightPercent: z.coerce.number().min(0).max(100),
});
type CategoryValues = z.infer<typeof categorySchema>;

function NewCategoryDialog({ courseId }: { courseId: string }) {
  const [open, setOpen] = useState(false);
  const createCategory = useCreateGradeCategory(courseId);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryValues>({ resolver: zodResolver(categorySchema) });

  function onSubmit(values: CategoryValues) {
    createCategory.mutate(values, {
      onSuccess: () => {
        reset();
        setOpen(false);
      },
    });
  }

  return (
    <Dialog open={open} onOpenChange={(next) => { setOpen(next); if (!next) reset(); }}>
      <DialogTrigger render={<Button variant="outline" size="sm">New category</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New grade category</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="category-name">Name</Label>
            <Input id="category-name" {...register("name")} />
            {errors.name ? <p className="text-xs text-destructive">{errors.name.message}</p> : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="category-weight">Weight (%)</Label>
            <Input id="category-weight" type="number" min={0} max={100} step="0.01" {...register("weightPercent")} />
            {errors.weightPercent ? (
              <p className="text-xs text-destructive">{errors.weightPercent.message}</p>
            ) : null}
          </div>
          <DialogFooter>
            <Button type="submit" disabled={createCategory.isPending}>
              {createCategory.isPending ? "Creating..." : "Create category"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function GradeCategoriesManager({ courseId, editable }: { courseId: string; editable: boolean }) {
  const categories = useGradeCategories(courseId);
  const deleteCategory = useDeleteGradeCategory(courseId);
  const totalWeight = categories.data?.reduce((sum, c) => sum + c.weightPercent, 0) ?? 0;

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-medium text-foreground">Grade categories</h2>
        {editable ? <NewCategoryDialog courseId={courseId} /> : null}
      </div>
      {categories.isLoading ? (
        <Skeleton className="h-16 w-full" />
      ) : categories.data && categories.data.length > 0 ? (
        <div className="flex flex-col gap-2">
          {categories.data.map((category) => (
            <Card key={category.id}>
              <CardContent className="flex items-center justify-between gap-4">
                <p className="text-sm font-medium text-foreground">
                  {category.name} <span className="text-muted-foreground">· {category.weightPercent}%</span>
                </p>
                {editable ? (
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={deleteCategory.isPending && deleteCategory.variables === category.id}
                    onClick={() => deleteCategory.mutate(category.id)}
                  >
                    Delete
                  </Button>
                ) : null}
              </CardContent>
            </Card>
          ))}
          {totalWeight !== 100 ? (
            <p className="text-xs text-muted-foreground">
              Weights currently total {totalWeight}% (not required to be exactly 100%).
            </p>
          ) : null}
        </div>
      ) : (
        <Card>
          <CardContent>
            <p className="text-sm text-muted-foreground">No grade categories yet.</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

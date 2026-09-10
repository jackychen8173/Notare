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
import { Textarea } from "@/components/ui/textarea";
import { useCreateMaterial, useDeleteMaterial, useMaterials, useMyCourseMaterials } from "@/hooks/useMaterials";
import type { Material } from "@/types/material";

const materialSchema = z.object({
  title: z.string().min(1, "Title is required"),
  description: z.string().optional(),
  url: z.string().optional(),
});
type MaterialValues = z.infer<typeof materialSchema>;

function NewMaterialDialog({ courseId }: { courseId: string }) {
  const [open, setOpen] = useState(false);
  const createMaterial = useCreateMaterial(courseId);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<MaterialValues>({ resolver: zodResolver(materialSchema) });

  function onSubmit(values: MaterialValues) {
    createMaterial.mutate(values, {
      onSuccess: () => {
        reset();
        setOpen(false);
      },
    });
  }

  return (
    <Dialog open={open} onOpenChange={(next) => { setOpen(next); if (!next) reset(); }}>
      <DialogTrigger render={<Button variant="outline" size="sm">New material</Button>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New material</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="material-title">Title</Label>
            <Input id="material-title" {...register("title")} />
            {errors.title ? <p className="text-xs text-destructive">{errors.title.message}</p> : null}
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="material-description">Description (optional)</Label>
            <Textarea id="material-description" rows={3} {...register("description")} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="material-url">Link (optional)</Label>
            <Input id="material-url" type="url" placeholder="https://..." {...register("url")} />
          </div>
          <DialogFooter>
            <Button type="submit" disabled={createMaterial.isPending}>
              {createMaterial.isPending ? "Adding..." : "Add material"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function MaterialCard({ material, editable, onDelete, deletePending }: {
  material: Material;
  editable: boolean;
  onDelete?: () => void;
  deletePending?: boolean;
}) {
  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-4">
        <div>
          <p className="font-medium text-foreground">{material.title}</p>
          {material.description ? (
            <p className="mt-1 text-sm text-muted-foreground">{material.description}</p>
          ) : null}
          {material.url ? (
            <a
              href={material.url}
              target="_blank"
              rel="noreferrer"
              className="mt-1 inline-block text-sm text-primary underline underline-offset-2"
            >
              {material.url}
            </a>
          ) : null}
        </div>
        {editable ? (
          <Button variant="outline" size="sm" disabled={deletePending} onClick={onDelete}>
            Delete
          </Button>
        ) : null}
      </CardContent>
    </Card>
  );
}

export function MaterialsSection({ courseId, editable }: { courseId: string; editable: boolean }) {
  const materials = useMaterials(courseId);
  const deleteMaterial = useDeleteMaterial(courseId);

  return (
    <div>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-medium text-foreground">Materials</h2>
        {editable ? <NewMaterialDialog courseId={courseId} /> : null}
      </div>
      {materials.isLoading ? (
        <Skeleton className="h-16 w-full" />
      ) : materials.data && materials.data.length > 0 ? (
        <div className="flex flex-col gap-2">
          {materials.data.map((material) => (
            <MaterialCard
              key={material.id}
              material={material}
              editable={editable}
              onDelete={() => deleteMaterial.mutate(material.id)}
              deletePending={deleteMaterial.isPending && deleteMaterial.variables === material.id}
            />
          ))}
        </div>
      ) : (
        <Card>
          <CardContent>
            <p className="text-sm text-muted-foreground">No materials yet.</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

export function StudentMaterialsSection({ courseId }: { courseId: string }) {
  const materials = useMyCourseMaterials(courseId);

  return (
    <div>
      <h2 className="mb-3 text-lg font-medium text-foreground">Materials</h2>
      {materials.isLoading ? (
        <Skeleton className="h-16 w-full" />
      ) : materials.data && materials.data.length > 0 ? (
        <div className="flex flex-col gap-2">
          {materials.data.map((material) => (
            <MaterialCard key={material.id} material={material} editable={false} />
          ))}
        </div>
      ) : (
        <Card>
          <CardContent>
            <p className="text-sm text-muted-foreground">No materials yet.</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

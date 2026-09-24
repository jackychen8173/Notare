"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { Controller, useForm } from "react-hook-form";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useCreateMaterial, useDeleteMaterial, useMaterials, useMyCourseMaterials } from "@/hooks/useMaterials";
import type { Material, MaterialType } from "@/types/material";

const MATERIAL_TYPE_LABELS: Record<MaterialType, string> = {
  LINK: "Link",
  GOOGLE_DOC: "Google Doc",
  GOOGLE_SLIDES: "Google Slides",
  PDF: "PDF",
};

// Extracts a Google file ID from any Google Docs/Slides share URL (.../d/{id}/edit?...) and builds
// the corresponding native embed URL. Returns null for PDF (embed the raw url directly - modern
// browsers render a PDF iframe src natively, no third-party viewer needed), LINK (no embed, same
// as today), or a Google URL that doesn't match the expected shape (falls back to a plain link
// rather than a broken iframe).
function buildEmbedUrl(type: MaterialType, url: string | null): string | null {
  if (!url) return null;
  if (type === "PDF") return url;
  if (type === "GOOGLE_DOC" || type === "GOOGLE_SLIDES") {
    const match = url.match(/\/d\/([a-zA-Z0-9_-]+)/);
    if (!match) return null;
    const id = match[1];
    return type === "GOOGLE_DOC"
      ? `https://docs.google.com/document/d/${id}/preview`
      : `https://docs.google.com/presentation/d/${id}/embed`;
  }
  return null;
}

const materialSchema = z.object({
  title: z.string().min(1, "Title is required"),
  description: z.string().optional(),
  url: z.string().optional(),
  type: z.enum(["LINK", "GOOGLE_DOC", "GOOGLE_SLIDES", "PDF"]),
});
type MaterialValues = z.infer<typeof materialSchema>;

function NewMaterialDialog({ courseId }: { courseId: string }) {
  const [open, setOpen] = useState(false);
  const createMaterial = useCreateMaterial(courseId);
  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<MaterialValues>({
    resolver: zodResolver(materialSchema),
    defaultValues: { type: "LINK" },
  });

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
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="material-type">Type</Label>
            <Controller
              control={control}
              name="type"
              render={({ field }) => (
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger id="material-type" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {(Object.keys(MATERIAL_TYPE_LABELS) as MaterialType[]).map((value) => (
                      <SelectItem key={value} value={value}>
                        {MATERIAL_TYPE_LABELS[value]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />
            <p className="text-xs text-muted-foreground">
              Google Doc/Slides/PDF render inline for students; Link just opens in a new tab.
            </p>
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
  const embedUrl = buildEmbedUrl(material.type, material.url);

  return (
    <Card>
      <CardContent className="flex flex-col gap-3">
        <div className="flex items-start justify-between gap-4">
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
        </div>
        {embedUrl ? (
          // The link above stays visible regardless - if the embed fails silently (a private doc,
          // revoked sharing), there's still a working way to open the material.
          <iframe
            src={embedUrl}
            title={material.title}
            className="h-[480px] w-full overflow-hidden rounded-card border-hairline border-border"
          />
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
